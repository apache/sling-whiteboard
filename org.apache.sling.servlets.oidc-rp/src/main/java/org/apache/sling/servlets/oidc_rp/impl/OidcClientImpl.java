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

import static org.apache.sling.servlets.oidc_rp.impl.OidcStateManager.PARAMETER_NAME_CONNECTION;
import static org.apache.sling.servlets.oidc_rp.impl.OidcStateManager.PARAMETER_NAME_REDIRECT;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.oidc_rp.OidcClient;
import org.apache.sling.servlets.oidc_rp.OidcConnection;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

@Component(service = { OidcClientImpl.class, OidcClient.class })
public class OidcClientImpl implements OidcClient {

    private final OidcProviderMetadataRegistry providerMetadataRegistry;

    @Activate
    public OidcClientImpl(@Reference OidcProviderMetadataRegistry providerMetadataRegistry) {
        this.providerMetadataRegistry = providerMetadataRegistry;
    }

    // TODO - can't make API when returning Nimbus Tokens
    public Tokens refreshAccessToken(OidcConnection connection, String refreshToken2) throws ParseException, IOException {

     // Construct the grant from the saved refresh token
     RefreshToken refreshToken = new RefreshToken(refreshToken2);
     AuthorizationGrant refreshTokenGrant = new RefreshTokenGrant(refreshToken);

     // The credentials to authenticate the client at the token endpoint
     ClientID clientID = new ClientID(connection.clientId());
     Secret clientSecret = new Secret(connection.clientSecret());
     ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

     // The token endpoint
     URI tokenEndpoint = providerMetadataRegistry.getProviderMetadata(connection.baseUrl()).getTokenEndpointURI();

     // Make the token request
     TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, refreshTokenGrant);

     TokenResponse response = TokenResponse.parse(request.toHTTPRequest().send());

     if (! response.indicatesSuccess()) {
         // We got an error response...
         TokenErrorResponse errorResponse = response.toErrorResponse();
         throw new RuntimeException("Failed refreshing the access token " + errorResponse.getErrorObject().getCode() + " : " + errorResponse.getErrorObject().getDescription());
     }

     AccessTokenResponse successResponse = response.toSuccessResponse();

     // Get the access token, the refresh token may be updated
     return successResponse.getTokens();
    }
    
    @Override
    public URI getOidcEntryPointUri(OidcConnection connection, SlingHttpServletRequest request, String redirectPath) {
        
        StringBuilder uri = new StringBuilder();
        uri.append(request.getScheme()).append("://").append(request.getServerName()).append(":").append(request.getServerPort())
            .append(OidcEntryPointServlet.PATH).append("?c=").append(connection.name());
        if ( redirectPath != null )
            uri.append("?redirect=").append(URLEncoder.encode(redirectPath, StandardCharsets.UTF_8));

        return URI.create(uri.toString());
    }

    @Override
    public URI getAuthenticationRequestUri(OidcConnection connection, SlingHttpServletRequest request, URI redirectUri) {
        OIDCProviderMetadata providerMetadata = providerMetadataRegistry.getProviderMetadata(connection.baseUrl());

        // The client ID provisioned by the OpenID provider when
        // the client was registered
        ClientID clientID = new ClientID(connection.clientId());

        // Generate random state string to securely pair the callback to this request
        State state = new State();
        OidcStateManager stateManager = OidcStateManager.stateFor(request);
        stateManager.registerState(state);
        stateManager.putAttribute(state, PARAMETER_NAME_CONNECTION, connection.name());
        if ( request.getParameter(PARAMETER_NAME_REDIRECT) != null )
            stateManager.putAttribute(state, PARAMETER_NAME_REDIRECT, request.getParameter(PARAMETER_NAME_REDIRECT));

        // Generate nonce for the ID token
        Nonce nonce = new Nonce();

        // Compose the OpenID authentication request (for the code flow)
        AuthenticationRequest authRequest = new AuthenticationRequest.Builder(
                new ResponseType("code"),
                new Scope(connection.scopes()),
                clientID, redirectUri)
            .endpointURI(providerMetadata.getAuthorizationEndpointURI())
            .state(state)
            .nonce(nonce)
            .customParameter("access_type", "offline") // request refresh token. TODO - is this Google-specific?
            .build();
        
        return authRequest.toURI();
    }
}
