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
package org.apache.sling.servlets.oidc_rp.impl;

import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.apache.sling.servlets.oidc_rp.OidcConnection;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.AuthenticationErrorResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationResponseParser;

@Component(service = { Servlet.class })
@SlingServletPaths(OidcCallbackServlet.PATH)
public class OidcCallbackServlet extends SlingAllMethodsServlet {

    static final String PATH = "/system/sling/oidc/callback";

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, OidcConnection> connections;
    private final OidcConnectionPersister persister;

    static String getCallbackUri(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + PATH;
    }

    @Activate
    public OidcCallbackServlet(@Reference(policyOption = GREEDY) List<OidcConnection> connections, @Reference OidcConnectionPersister persister) {
        this.connections = connections.stream()
                .collect(Collectors.toMap( OidcConnection::name, Function.identity()));
        this.persister = persister;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        try {
            StringBuffer requestURL = request.getRequestURL();
            if ( request.getQueryString() != null )
                requestURL.append('?').append(request.getQueryString());

            AuthenticationResponse authResponse = AuthenticationResponseParser.parse(new URI(requestURL.toString()));
            OidcStateManager stateManager = OidcStateManager.stateFor(request);
            if ( authResponse.getState() == null || !stateManager.isValidState(authResponse.getState()))
                throw new ServletException("Failed state check");

            if ( response instanceof AuthenticationErrorResponse )
                throw new ServletException(authResponse.toErrorResponse().getErrorObject().toString());

            String authCode = authResponse.toSuccessResponse().getAuthorizationCode().getValue();
            
            Optional<String> desiredConnectionName = stateManager.getStateAttribute(authResponse.getState(), OidcStateManager.PARAMETER_NAME_CONNECTION);
            if ( desiredConnectionName.isEmpty() ) {
                logger.debug("Did not find any connection in stateManager");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);  
                return;
            }
            OidcConnection connection = connections.get(desiredConnectionName.get());
            if ( connection == null ) {
                logger.debug("Requested unknown connection {}", desiredConnectionName.get());
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if ( connection.baseUrl() == null )
                throw new ServletException("Misconfigured baseUrl");
            
            // TODO - this code should be extracted and reused to refresh the access token with a refresh token, if present
            HttpClient client = HttpClient.newHttpClient();
            Endpoints ep = Endpoints.discover(connection.baseUrl(), client);

            Map<String, String> params = new HashMap<>();
            params.put("client_id", connection.clientId());
            params.put("client_secret", connection.clientSecret());
            params.put("code", authCode);
            params.put("grant_type", "authorization_code");
            params.put("redirect_uri", getCallbackUri(request));

            String payload = params.entrySet().stream()
                    .map( e -> e.getKey() + "=" +URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ep.tokenEndpoint()))
                    .headers("Content-Type", "application/x-www-form-urlencoded")
                    .POST(BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> tokenResponse = client.send(tokenRequest, BodyHandlers.ofString());
            Optional<String> redirect = stateManager.getStateAttribute(authResponse.getState(), OidcStateManager.PARAMETER_NAME_REDIRECT);
            stateManager.unregisterState(authResponse.getState());

            String accessToken;
            String refreshToken = null;
            ZonedDateTime expiry = null;

            try ( JsonReader reader = Json.createReader(new StringReader(tokenResponse.body())) ) {
                JsonObject tokenObject = reader.readObject();
                accessToken = tokenObject.getString("access_token");
                JsonNumber expiresIn = tokenObject.getJsonNumber("expires_in");
                if ( expiresIn != null ) {
                    expiry = LocalDateTime.now().plus(expiresIn.intValue(), ChronoUnit.SECONDS).atZone(ZoneId.systemDefault());
                }
                refreshToken = tokenObject.getString("refresh_token", null);
            }

            persister.persistToken(connection, request.getResourceResolver(), accessToken, refreshToken, expiry);

            if ( redirect.isEmpty() ) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.addHeader("Content-Type", "text/plain");
                response.getWriter().write("OK");
                response.getWriter().flush();
                response.getWriter().close();
            } else {
                response.sendRedirect(URLDecoder.decode(redirect.get(), StandardCharsets.UTF_8));
            }
        } catch (ParseException | URISyntaxException | IOException e) {
            throw new ServletException(e);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
        }
    }

}
