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

import java.io.IOException;
import java.net.URI;

import org.apache.sling.extensions.oidc_rp.OAuthException;
import org.apache.sling.extensions.oidc_rp.OAuthTokenRefresher;
import org.apache.sling.extensions.oidc_rp.OAuthTokens;
import org.apache.sling.extensions.oidc_rp.OidcConnection;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;

@Component
public class OAuthTokenRefresherImpl implements OAuthTokenRefresher {

    private final OidcProviderMetadataRegistry providerMetadataRegistry;

    @Activate
    public OAuthTokenRefresherImpl(@Reference OidcProviderMetadataRegistry providerMetadataRegistry) {
        this.providerMetadataRegistry = providerMetadataRegistry;
    }
    
    @Override
    public OAuthTokens refreshTokens(OidcConnection connection, String refreshToken2) {
        return Converter.toSlingOAuthTokens(refreshTokensInternal(connection, refreshToken2));
    }
    
    public Tokens refreshTokensInternal(OidcConnection connection, String refreshToken2) {

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
            
             AccessTokenResponse response = AccessTokenResponse.parse(request.toHTTPRequest().send());
            
             if (! response.indicatesSuccess()) {
                 // We got an error response...
                 TokenErrorResponse errorResponse = response.toErrorResponse();
                 throw new OAuthException("Failed refreshing the access token " + errorResponse.getErrorObject().getCode() + " : " + errorResponse.getErrorObject().getDescription());
             }
            
             AccessTokenResponse successResponse = response.toSuccessResponse();
            
             // Get the access token, the refresh token may be updated
             return successResponse.getTokens();
        } catch (ParseException |IOException e) {
            throw new OAuthException(e);
        }
    }
}
