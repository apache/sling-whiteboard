/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets.oidc_rp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.exceptions.TestingValidationException;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.SignedJWT;

import dasniko.testcontainers.keycloak.KeycloakContainer;

class AuthorizationCodeFlowIT {

    private static final String OIDC_CONFIG_PID = "org.apache.sling.servlets.oidc_rp.impl.OidcConnectionImpl";

    private KeycloakContainer keycloak;
    private SlingClient sling;

    private int keycloakPort;

    private String oidcOsgiConfigPid;

    @BeforeEach
    @SuppressWarnings("resource")
    void initKeycloak() {
        // support using an existing Keycloak instance by setting
        // KEYCLOAK_URL=http://localhost:24098/
        // this is most usually done in an IDE, with both Keycloak and Sling running
        String existingKeyCloakUrl = System.getenv("KEYCLOAK_URL");
        if ( existingKeyCloakUrl == null ) {
            keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:20.0.3")
                    .withRealmImportFile("keycloak-import/sling.json");
            keycloak.start();
            keycloakPort = keycloak.getHttpPort();
        } else {
            keycloakPort = URI.create(existingKeyCloakUrl).getPort(); 
        }
    }

    @BeforeEach
    void initSling() throws ClientException {

        int slingPort = Integer.getInteger("sling.http.port", 8080);
        sling = SlingClient.Builder.create(URI.create("http://localhost:" + slingPort), "admin", "admin").disableRedirectHandling().build();

        // ensure all previous connections are cleaned up
        sling.adaptTo(OsgiConsoleClient.class).deleteConfiguration(OIDC_CONFIG_PID + ".keycloak");
    }

    @AfterEach
    void shutdownKeycloak() {
        if ( keycloak != null )
            keycloak.close();
    }
    
    @AfterEach
    void cleanupSlingOidcConfig() throws TestingValidationException, ClientException {

        // the Sling testing clients do not offer a way of listing configurations, as assigned PIDs
        // are not predictable. So instead of running deleting test configs when the test starts
        // we fall back to cleaning after, which is hopefully reliable enough
        if ( oidcOsgiConfigPid != null )
            sling.adaptTo(OsgiConsoleClient.class).deleteConfiguration(oidcOsgiConfigPid);
    }

    @Test
    void accessTokenIsPresentOnSuccessfulLogin() throws Exception {
        
        String oidcConnectionName = "keycloak";

        // configure connection to keycloak
        oidcOsgiConfigPid = sling.adaptTo(OsgiConsoleClient.class).editConfiguration(OIDC_CONFIG_PID+ ".keycloak",OIDC_CONFIG_PID, 
                Map.of(
                    "name", oidcConnectionName, 
                    "baseUrl", "http://localhost:" + keycloakPort+"/realms/sling",
                    "clientId", "oidc-test",
                    "clientSecret", "wM2XIbxBTLJAac2rJSuHyKaoP8IWvSwJ",
                    "scopes", "openid"
                )
            );
        
        // clean up any existing tokens
        String userPath = getUserPath(sling, sling.getUser());
        sling.deletePath(userPath + "/oidc-tokens/" + oidcConnectionName, 200);
        sling.doGet(userPath + "/oidc-tokens/" + oidcConnectionName, 404);
        
        // kick off oidc auth
        SlingHttpResponse entryPointResponse = sling.doGet("/system/sling/oidc/entry-point", List.of(new BasicNameValuePair("c", oidcConnectionName)), 302);
        Header locationHeader = entryPointResponse.getFirstHeader("location");
        assertThat(locationHeader.getElements()).as("Location header value from entry-point request")
            .singleElement().asString().startsWith("http://localhost:" + keycloakPort);
        
        String locationHeaderValue = locationHeader.getValue();
        
        // load login form from keycloak
        HttpClient keycloak = HttpClient.newHttpClient();        
        HttpRequest renderLoginFormRequest = HttpRequest.newBuilder().uri(URI.create(locationHeaderValue)).build();
        HttpResponse<Stream<String>> renderLoginFormResponse = keycloak.send(renderLoginFormRequest, BodyHandlers.ofLines());
        List<String> matchingFormLines = renderLoginFormResponse.body()
            .filter( line -> line.contains("id=\"kc-form-login\""))
            .collect(Collectors.toList());
        assertThat(matchingFormLines).as("lines matching form id").singleElement();
        String formLine = matchingFormLines.get(0);
        int actionAttrStart = formLine.indexOf("action=\"") + "action=\"".length();
        int actionAttrEnd = formLine.indexOf('"', actionAttrStart);
        
        String actionAttr = formLine.substring(actionAttrStart, actionAttrEnd).replace("&amp;", "&");
        
        List<String> authFormRequestCookies = renderLoginFormResponse.headers().allValues("set-cookie");
        
        Map<String, String> authData = Map.of("username", "test", "password", "test", "credentialId", "");
        String requestBody = authData.entrySet().stream()
            .map( e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
        
        HttpRequest.Builder authenticateRequest = HttpRequest.newBuilder(URI.create(actionAttr))
                .POST(BodyPublishers.ofString(requestBody))
                .header("content-type", "application/x-www-form-urlencoded");
        authFormRequestCookies.stream().forEach( cookie -> authenticateRequest.header("cookie", cookie));
        
        HttpResponse<String> authenticateResponse = keycloak.send(authenticateRequest.build(), BodyHandlers.ofString());
        System.out.println(authenticateResponse.body());
        Optional<String> authResponseLocationHeader = authenticateResponse.headers().firstValue("location");
        assertThat(authResponseLocationHeader).as("Authentication response header").isPresent();
        
        URI redirectUri = URI.create(authResponseLocationHeader.get());
        System.out.println(redirectUri.getRawPath()+"?" + redirectUri.getRawQuery());
        List<NameValuePair> params = Arrays.stream(redirectUri.getRawQuery().split("&"))
            .map( s -> {
                var parts = s.split("=");
                return (NameValuePair) new BasicNameValuePair(parts[0], parts[1]);
            })
            .collect(Collectors.toList());
        sling.doGet(redirectUri.getRawPath(), params, 200);
        
        JsonNode keycloakToken = sling.doGetJson(userPath + "/oidc-tokens/" + oidcConnectionName,0,  200);
        String accesToken = keycloakToken.get("access_token").asText();
        // validate that the JWT is valid; we trust what keycloak has returned but just want to ensure that
        // the token was stored correctly
        SignedJWT.parse(accesToken);
    }

    private String getUserPath(SlingClient sling, String authorizableId) throws ClientException {
        
        ObjectNode usersJson = (ObjectNode) sling.doGetJson("/home/users", 2, 200);
        for ( Map.Entry<String,JsonNode> user : toIterable(usersJson.fields()) ) {
            JsonNode jsonNode = user.getValue().get("jcr:primaryType");
            if ( jsonNode == null )
                continue;
            
            if ( jsonNode.isTextual() && "rep:AuthorizableFolder".equals(jsonNode.asText())) {
                ObjectNode node = (ObjectNode) user.getValue();
                for ( Map.Entry<String, JsonNode> user2 : toIterable(node.fields()) ) {
                    JsonNode primaryType = user2.getValue().get("jcr:primaryType");
                    if ( primaryType != null && primaryType.isTextual() && primaryType.asText().equals("rep:User")) {
                        JsonNode authorizableIdProp = user2.getValue().get("rep:authorizableId");
                        if (authorizableId.equals(authorizableIdProp.asText()) )
                            return "/home/users/" + user.getKey() + "/" + user2.getKey();
                    }
                }
            }
        }
        
        throw new IllegalArgumentException(String.format("Unable to locate path for user with id '%s'", authorizableId));
    }

    private static <T> Iterable<T> toIterable(Iterator<T> iterator) {
        return () -> iterator;
    }

}
