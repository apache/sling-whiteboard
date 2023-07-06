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

import java.net.URI;
import java.util.Collections;

import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

@ExtendWith(SlingContextExtension.class)
class OidcClientImplTest {

    private final SlingContext context = new SlingContext();
    
    @Test
    void testRedirectUri() {
        OidcClientImpl clientImpl = new OidcClientImpl(null);
        URI redirectUri = clientImpl.getOidcEntryPointUri(MockOidcConnection.DEFAULT_CONNECTION, context.request(), "/foo");
        
        assertThat(redirectUri).as("redirect uri")
            .hasScheme("http")    
            .hasHost("localhost")
            .hasNoPort()
            .hasPath("/system/sling/oidc/entry-point")
            .hasQuery("c=mock-oidc&redirect=/foo");
    }

    @Test
    void testRedirectUri_customPort_noRedirect() {
        OidcClientImpl clientImpl = new OidcClientImpl(null);
        context.request().setServerPort(8080);
        URI redirectUri = clientImpl.getOidcEntryPointUri(MockOidcConnection.DEFAULT_CONNECTION, context.request(), null);
        
        assertThat(redirectUri).as("redirect uri")
            .hasScheme("http")    
            .hasHost("localhost")
            .hasPort(8080)
            .hasPath("/system/sling/oidc/entry-point")
            .hasQuery("c=mock-oidc");
    }
    
    @Test
    void testGetAuthenticationRequestUri() {
    
        OidcClientImpl clientImpl = new OidcClientImpl(new OidcProviderMetadataRegistry() {
            @Override
            public OIDCProviderMetadata getProviderMetadata(String base) {
                return new OIDCProviderMetadata(new Issuer(base), Collections.singletonList(SubjectType.PUBLIC), URI.create("https://foo.example/jwks")) {
                    @Override
                    public URI getAuthorizationEndpointURI() {
                        return URI.create("https://foo.example/authz");
                    }
                };
            }
        });
        URI requestUri = clientImpl.getAuthenticationRequestUri(MockOidcConnection.DEFAULT_CONNECTION, context.request(), URI.create("http://localhost/callback"));

        assertThat(requestUri).as("authentication request uri")
            .hasScheme("https")
            .hasHost("foo.example")
            .hasPath("/authz")
            .hasParameter("scope", "openid")
            .hasParameter("response_type", "code")
            .hasParameter("client_id", "client-id")
            .hasParameter("redirect_uri", "http://localhost/callback")
            .hasParameter("nonce")
            .hasParameter("state");
        
        
    }

}
