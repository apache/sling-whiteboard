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
package org.apache.sling.extensions.oauth_client.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.sling.extensions.oauth_client.OidcTokens;
import org.apache.sling.extensions.oauth_client.impl.Converter;
import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

class ConverterTest {

    @Test
    void convertToApiTokens_allSet() {
        OIDCTokens nimbusTokens = new OIDCTokens("idToken", new BearerAccessToken("accessToken", 100, null), new RefreshToken("refreshToken"));
        
        OidcTokens apiTokens = Converter.toApiOidcTokens(nimbusTokens);
        
        assertThat(apiTokens.accessToken()).isEqualTo("accessToken");
        assertThat(apiTokens.expiresAt()).isEqualTo(100);
        assertThat(apiTokens.refreshToken()).isEqualTo("refreshToken");
        assertThat(apiTokens.idToken()).isEqualTo("idToken");
    }


    @Test
    void convertToApiTokens_accessOnly() {
        OIDCTokens nimbusTokens = new OIDCTokens(new BearerAccessToken("accessToken"), null);
        
        OidcTokens apiTokens = Converter.toApiOidcTokens(nimbusTokens);
        
        assertThat(apiTokens.accessToken()).isEqualTo("accessToken");
        assertThat(apiTokens.expiresAt()).isZero();
        assertThat(apiTokens.refreshToken()).isNull();
        assertThat(apiTokens.idToken()).isNull();
    }
    
    @Test
    void convertToNimbusTokens_allSet() {
        
        OidcTokens apiTokens = new OidcTokens("accessToken", 100, "refreshToken", "idToken");
        
        OIDCTokens nimbusTokens = Converter.toNimbusOidcTokens(apiTokens);
        
        assertThat(nimbusTokens.getAccessToken()).as("access token")
            .isNotNull()
            .extracting(AccessToken::getValue, AccessToken::getLifetime)
            .containsExactly("accessToken", 100l);
        
        assertThat(nimbusTokens.getRefreshToken()).as("refresh token")
            .isNotNull()
            .extracting(RefreshToken::getValue)
            .isEqualTo("refreshToken");
        
        assertThat(nimbusTokens.getIDTokenString()).as("id token")
            .isNotNull()
            .isEqualTo("idToken");
    }
    
    @Test
    void convertToNimbusTokens_accessOnly() {

        OidcTokens apiTokens = new OidcTokens("accessToken", 0, null, null);
        
        OIDCTokens nimbusTokens = Converter.toNimbusOidcTokens(apiTokens);
        
        assertThat(nimbusTokens.getAccessToken()).as("access token")
            .isNotNull()
            .extracting(AccessToken::getValue, AccessToken::getLifetime)
            .containsExactly("accessToken", 0l);
        
        assertThat(nimbusTokens.getRefreshToken()).as("refresh token")
            .isNull();
        
        assertThat(nimbusTokens.getIDTokenString()).as("id token")
            .isNull();
    }
}
