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
package org.apache.sling.extensions.oidc_rp.impl;

import static org.apache.sling.extensions.oidc_rp.impl.OidcStateManager.PARAMETER_NAME_CONNECTION;
import static org.apache.sling.extensions.oidc_rp.impl.OidcStateManager.PARAMETER_NAME_REDIRECT;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.extensions.oidc_rp.OidcClient;
import org.apache.sling.extensions.oidc_rp.OidcConnection;
import org.apache.sling.extensions.oidc_rp.OidcException;
import org.apache.sling.extensions.oidc_rp.OidcTokens;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

@Component(service = { OidcClientImpl.class, OidcClient.class })
public class OidcClientImpl implements OidcClient {

    private final OidcProviderMetadataRegistry providerMetadataRegistry;

    @Activate
    public OidcClientImpl(@Reference OidcProviderMetadataRegistry providerMetadataRegistry) {
        this.providerMetadataRegistry = providerMetadataRegistry;
    }
    
    @Override
    public OidcTokens refreshTokens(OidcConnection connection, String refreshToken2) {
        return Converter.toApiOidcTokens(refreshTokensInternal(connection, refreshToken2));
    }
    
    public OIDCTokens refreshTokensInternal(OidcConnection connection, String refreshToken2) {

     try {
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
        
         OIDCTokenResponse response = OIDCTokenResponse.parse(request.toHTTPRequest().send());
        
         if (! response.indicatesSuccess()) {
             // We got an error response...
             TokenErrorResponse errorResponse = response.toErrorResponse();
             throw new OidcException("Failed refreshing the access token " + errorResponse.getErrorObject().getCode() + " : " + errorResponse.getErrorObject().getDescription());
         }
        
         OIDCTokenResponse successResponse = response.toSuccessResponse();
        
         // Get the access token, the refresh token may be updated
         return successResponse.getOIDCTokens();
    } catch (ParseException |IOException e) {
        throw new OidcException(e);
    }
    }
    
    @Override
    public URI getOidcEntryPointUri(OidcConnection connection, SlingHttpServletRequest request, String redirectPath) {
        
        StringBuilder uri = new StringBuilder();
        uri.append(request.getScheme()).append("://").append(request.getServerName());
        boolean needsExplicitPort = ( "https".equals(request.getScheme()) && request.getServerPort() != 443 )
                || ( "http".equals(request.getScheme()) && request.getServerPort() != 80 ) ;
                
        if ( needsExplicitPort ) {
            uri.append(':').append(request.getServerPort());
        }
        uri.append(OidcEntryPointServlet.PATH).append("?c=").append(connection.name());
        if ( redirectPath != null )
            uri.append("&redirect=").append(URLEncoder.encode(redirectPath, StandardCharsets.UTF_8));

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
        AuthenticationRequest.Builder authRequestBuilder = new AuthenticationRequest.Builder(
                new ResponseType("code"),
                new Scope(connection.scopes()),
                clientID, URI.create(OidcCallbackServlet.getCallbackUri(request)))
            .endpointURI(providerMetadata.getAuthorizationEndpointURI())
            .state(state)
            .nonce(nonce);
        
        if ( connection.additionalAuthorizationParameters() != null ) {
            Arrays.stream(connection.additionalAuthorizationParameters())
                .map( s -> s.split("=") )
                .filter( p -> p.length == 2 )
                .forEach( p -> authRequestBuilder.customParameter(p[0], p[1]));
        }
        
        return authRequestBuilder.build().toURI();
    }
}
