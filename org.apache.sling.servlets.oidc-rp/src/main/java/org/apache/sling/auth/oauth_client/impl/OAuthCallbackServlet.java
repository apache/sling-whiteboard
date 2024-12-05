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
package org.apache.sling.auth.oauth_client.impl;

import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.auth.oauth_client.ClientConnection;
import org.apache.sling.auth.oauth_client.OAuthTokenStore;
import org.apache.sling.auth.oauth_client.OAuthTokens;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ErrorResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;

@Component(service = { Servlet.class },
    property = { AuthConstants.AUTH_REQUIREMENTS +"=" + OAuthCallbackServlet.PATH }
)
@SlingServletPaths(OAuthCallbackServlet.PATH)
public class OAuthCallbackServlet extends SlingAllMethodsServlet {

    static final String PATH = "/system/sling/oauth/callback";

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final Map<String, ClientConnection> connections;
    private final OAuthTokenStore tokenStore;
    private final OAuthStateManager stateManager;

    static String getCallbackUri(HttpServletRequest request) {
        String portFragment = "";
        boolean isNonDefaultHttpPort = request.getScheme().equals("http") && request.getServerPort() != 80;
        boolean isNonDefaultHttpsPort = request.getScheme().equals("https") && request.getServerPort() != 443;
        if ( isNonDefaultHttpPort || isNonDefaultHttpsPort )
            portFragment = ":" + request.getServerPort();

        return request.getScheme() + "://" + request.getServerName() + portFragment + PATH;
    }
    
    private static String toErrorMessage(String context, ErrorResponse error) {
        
        ErrorObject errorObject = error.getErrorObject();
        StringBuilder message = new StringBuilder();
        
        message.append(context)
            .append(": ")
            .append(errorObject.getCode());
        
        message.append(". Status code: ").append(errorObject.getHTTPStatusCode());
        
        String description = errorObject.getDescription();
        if ( description != null )
           message.append(". ").append(description);
       
        return message.toString();
    }

    @Activate
    public OAuthCallbackServlet(@Reference(policyOption = GREEDY) List<ClientConnection> connections, 
            @Reference OAuthTokenStore tokenStore,
            @Reference OAuthStateManager stateManager) {
        this.connections = connections.stream()
                .collect(Collectors.toMap( ClientConnection::name, Function.identity()));
        this.tokenStore = tokenStore;
        this.stateManager = stateManager;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        StringBuffer requestURL = request.getRequestURL();
        if ( request.getQueryString() != null )
            requestURL.append('?').append(request.getQueryString());

        AuthorizationResponse authResponse;
        Optional<OAuthState>  clientState;
        Cookie stateCookie;
        try {
            authResponse = AuthorizationResponse.parse(new URI(requestURL.toString()));
            
            clientState = stateManager.toOAuthState(authResponse.getState());
            if ( !clientState.isPresent() )  {
                logger.debug("Failed state check: no state found in authorization response");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            stateCookie = request.getCookie(OAuthStateManager.COOKIE_NAME_REQUEST_KEY);
            if ( stateCookie == null ) {
                logger.debug("Failed state check: No request cookie named '{}' found", OAuthStateManager.COOKIE_NAME_REQUEST_KEY);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

        } catch (ParseException | URISyntaxException e) {
            logger.debug("Failed to parse authorization response", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {            
            String stateFromAuthServer = clientState.get().perRequestKey();
            String stateFromClient = stateCookie.getValue();
            if ( ! stateFromAuthServer.equals(stateFromClient) )
                throw new IllegalStateException("Failed state check: request keys from client and server are not the same");

            if ( !authResponse.indicatesSuccess() ) {
                AuthorizationErrorResponse errorResponse = authResponse.toErrorResponse();
                throw new OAuthCallbackException("Authentication failed", new RuntimeException(toErrorMessage("Error in authentication response", errorResponse)));
            }

            Optional<String> redirect = Optional.ofNullable(clientState.get().redirect());
            
            String authCode = authResponse.toSuccessResponse().getAuthorizationCode().getValue();
            
            String desiredConnectionName = clientState.get().connectionName();
            if ( desiredConnectionName == null || desiredConnectionName.isEmpty() )
                throw new IllegalArgumentException("No connection found in clientState");
            
            ClientConnection connection = connections.get(desiredConnectionName);
            if ( connection == null )
                throw new IllegalArgumentException(String.format("Requested unknown connection '%s'", desiredConnectionName));
            
            ResolvedOAuthConnection conn = ResolvedOAuthConnection.resolve(connection);

            ClientID clientId = new ClientID(conn.clientId());
            Secret clientSecret = new Secret(conn.clientSecret());
            ClientSecretBasic clientCredentials = new ClientSecretBasic(clientId, clientSecret);
            
            AuthorizationCode code = new AuthorizationCode(authCode);
            
            URI tokenEndpoint = new URI(conn.tokenEndpoint());
            
            TokenRequest tokenRequest = new TokenRequest.Builder(
                tokenEndpoint,
                clientCredentials,
                new AuthorizationCodeGrant(code, new URI(getCallbackUri(request)))
            ).build();
            
            HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
            // GitHub requires an explicitly set Accept header, otherwise the response is url encoded
            // https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#2-users-are-redirected-back-to-your-site-by-github
            // see also https://bitbucket.org/connect2id/oauth-2.0-sdk-with-openid-connect-extensions/issues/107/support-application-x-www-form-urlencoded
            httpRequest.setAccept("application/json");
            HTTPResponse httpResponse = httpRequest.send();
            
            TokenResponse tokenResponse = TokenResponse.parse(httpResponse);
            
            if ( !tokenResponse.indicatesSuccess() )
                throw new OAuthCallbackException("Token exchange error", new RuntimeException(toErrorMessage("Error in token response", tokenResponse.toErrorResponse())));

            OAuthTokens tokens = Converter.toSlingOAuthTokens(tokenResponse.toSuccessResponse().getTokens());
            
            tokenStore.persistTokens(connection, request.getResourceResolver(), tokens);

            if ( redirect.isEmpty() ) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.sendRedirect(URLDecoder.decode(redirect.get(), StandardCharsets.UTF_8));
            }

        } catch (IllegalStateException e) {
            throw new OAuthCallbackException("State check failed", e);
        } catch (IllegalArgumentException e) {
            throw new OAuthCallbackException("Internal error", e);
        } catch (ParseException e) {
            throw new OAuthCallbackException("Invalid invocation", e);
        } catch ( OAuthCallbackException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuthCallbackException("Unknown error", e);
        }
    }
}
