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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jackrabbit.usermanager.impl.AuthorizableAdapterFactory;
import org.apache.sling.servlets.oidc_rp.OidcConnection;
import org.apache.sling.servlets.oidc_rp.OidcToken;
import org.apache.sling.servlets.oidc_rp.OidcTokenState;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

@ExtendWith(SlingContextExtension.class)
class OidcConnectionFinderImplTest {

    private final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    private final MockOidcConnection connection = new MockOidcConnection(new String[] {"openid"}, "mock-oidc", "client-id", "client-secret", "http://example.com");

    @BeforeEach
    public void registerAdapterFactories() {
        context.registerInjectActivateService(new AuthorizableAdapterFactory());
    }

    @Test
    void persistTokens_accessTokenOnly() throws LoginException, RepositoryException {

        OIDCTokens tokens = new OIDCTokens(new BearerAccessToken(12), null);

        OidcConnectionFinderImpl connectionFinder = new OidcConnectionFinderImpl();
        connectionFinder.persistTokens(connection, context.resourceResolver(), tokens);

        Resource connectionResource = getConnectionResource(connection);

        ValueMap connectionProps = connectionResource.getValueMap();
        assertThat(connectionProps)
            .as("stored tokens for connection")
            .containsOnlyKeys("jcr:primaryType", "access_token")
            .containsEntry("access_token", tokens.getAccessToken().getValue());
    }

    @Test
    void persistTokens_accessAndIdToken() throws LoginException, RepositoryException {

        OIDCTokens tokens = new OIDCTokens(new PlainJWT(new JWTClaimsSet.Builder().issuer("example.com").build()), new BearerAccessToken(12), null);

        OidcConnectionFinderImpl connectionFinder = new OidcConnectionFinderImpl();
        connectionFinder.persistTokens(connection, context.resourceResolver(), tokens);

        Resource connectionResource = getConnectionResource(connection);

        ValueMap connectionProps = connectionResource.getValueMap();
        assertThat(connectionProps)
            .as("stored tokens for connection")
            .containsOnlyKeys("jcr:primaryType", "access_token", "id_token")
            .containsEntry("access_token", tokens.getAccessToken().getValue());
    }

    
    @Test
    void getAccessToken_missing() {
        
        OidcConnectionFinderImpl connectionFinder = new OidcConnectionFinderImpl();
        
        OidcToken accessToken = connectionFinder.getAccessToken(connection, context.resourceResolver());
        
        assertThat(accessToken).as("access token")
            .isNotNull()
            .extracting( OidcToken::getState )
            .isEqualTo( OidcTokenState.MISSING );
    }
    
    @Test
    void getAccessToken_valid() {
        
        OIDCTokens tokens = new OIDCTokens(new BearerAccessToken(12), null);

        OidcConnectionFinderImpl connectionFinder = new OidcConnectionFinderImpl();
        connectionFinder.persistTokens(connection, context.resourceResolver(), tokens);
        
        OidcToken accessToken = connectionFinder.getAccessToken(connection, context.resourceResolver());
        assertThat(accessToken).as("access token")
            .isNotNull()
            .extracting( OidcToken::getState , OidcToken::getValue )
            .containsExactly( OidcTokenState.VALID, tokens.getAccessToken().getValue() );
    }

    @Test
    void getAccessToken_expired() throws InterruptedException {
        
        int lifetimeSeconds = 1;
        OIDCTokens tokens = new OIDCTokens(new BearerAccessToken(12, lifetimeSeconds, null), null);
        
        OidcConnectionFinderImpl connectionFinder = new OidcConnectionFinderImpl();
        connectionFinder.persistTokens(connection, context.resourceResolver(), tokens);

        // wait for the token to expire
        Thread.sleep( TimeUnit.SECONDS.toMillis( 2 * lifetimeSeconds ) );
        
        OidcToken accessToken = connectionFinder.getAccessToken(connection, context.resourceResolver());
        assertThat(accessToken).as("access token")
            .isNotNull()
            .extracting( OidcToken::getState )
            .isEqualTo( OidcTokenState.EXPIRED );
    }
    
    @Test
    void getRefreshToken_valid() {
        OIDCTokens tokens = new OIDCTokens(new BearerAccessToken(12), new RefreshToken(12));

        OidcConnectionFinderImpl connectionFinder = new OidcConnectionFinderImpl();
        connectionFinder.persistTokens(connection, context.resourceResolver(), tokens);
        
        OidcToken refreshToken = connectionFinder.getRefreshToken(connection, context.resourceResolver());
        assertThat(refreshToken).as("refresh token")
            .isNotNull()
            .extracting( OidcToken::getState , OidcToken::getValue )
            .containsExactly( OidcTokenState.VALID, tokens.getRefreshToken().getValue() );
    }
    
    @Test
    void getRefreshToken_missing() {
        
        OidcConnectionFinderImpl connectionFinder = new OidcConnectionFinderImpl();
        
        OidcToken refreshToken = connectionFinder.getRefreshToken(connection, context.resourceResolver());
        assertThat(refreshToken).as("refresh token")
            .isNotNull()
            .extracting( OidcToken::getState )
            .isEqualTo( OidcTokenState.MISSING);
    }

    private Resource getConnectionResource(OidcConnection connection) throws RepositoryException {
        String userPath = context.resourceResolver().adaptTo(User.class).getPath();
        Resource userHomeResource = context.resourceResolver().getResource(userPath);
        Resource oidcTokensResource = userHomeResource.getChild("oidc-tokens");

        assertThat(oidcTokensResource)
            .describedAs("oidc-tokens resource")
            .isNotNull();

        Resource connectionResource = oidcTokensResource.getChild(connection.name());
        assertThat(connectionResource)
            .as("oidc-tokens/connection resource")
            .isNotNull();
        return connectionResource;
    }

    public record MockOidcConnection(String[] scopes, String name, String clientId, String clientSecret, String baseUrl) implements OidcConnection { }
}
