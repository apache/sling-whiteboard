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

import org.apache.sling.auth.oauth_client.OAuthTokens;
import org.apache.sling.auth.oauth_client.OidcTokens;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

public class Converter {

    public static OIDCTokens toNimbusOidcTokens(OidcTokens tokens) {
        OIDCTokens nimbusTokens;
        RefreshToken nimbusRefreshToken = tokens.refreshToken() != null ? new RefreshToken(tokens.refreshToken()) : null;
        BearerAccessToken nimbusAccessToken = new BearerAccessToken(tokens.accessToken(), tokens.expiresAt(), null);
        if ( tokens.idToken() != null ) {
            nimbusTokens = new OIDCTokens(tokens.idToken(), nimbusAccessToken, nimbusRefreshToken); 
        } else {
            nimbusTokens = new OIDCTokens(nimbusAccessToken, nimbusRefreshToken);
        }
        
        return nimbusTokens;
    }
    
    public static OidcTokens toApiOidcTokens(OIDCTokens nimbusTokens) {
        String accessToken = nimbusTokens.getAccessToken() != null ? nimbusTokens.getAccessToken().getValue() : null;
        long expiresAt = nimbusTokens.getAccessToken() != null ? nimbusTokens.getAccessToken().getLifetime() : 0;
        String refreshToken = nimbusTokens.getRefreshToken() != null ? nimbusTokens.getRefreshToken().getValue() : null;
        String idToken = nimbusTokens.getIDTokenString();
        
        return new OidcTokens(accessToken, expiresAt, refreshToken, idToken);
    }
    
    public static OAuthTokens toSlingOAuthTokens(OIDCTokens nimbusTokens) {
        String accessToken = nimbusTokens.getAccessToken() != null ? nimbusTokens.getAccessToken().getValue() : null;
        long expiresAt = nimbusTokens.getAccessToken() != null ? nimbusTokens.getAccessToken().getLifetime() : 0;
        String refreshToken = nimbusTokens.getRefreshToken() != null ? nimbusTokens.getRefreshToken().getValue() : null;
        
        return new OAuthTokens(accessToken, expiresAt, refreshToken);        
    }
    
    public static OAuthTokens toSlingOAuthTokens(Tokens oAuthTokens) {
        String accessToken = oAuthTokens.getAccessToken() != null ? oAuthTokens.getAccessToken().getValue() : null;
        long expiresAt = oAuthTokens.getAccessToken() != null ? oAuthTokens.getAccessToken().getLifetime() : 0;
        String refreshToken = oAuthTokens.getRefreshToken() != null ? oAuthTokens.getRefreshToken().getValue() : null;

        return new OAuthTokens(accessToken, expiresAt, refreshToken);
    }
    
    private Converter() {
        
    }
}
