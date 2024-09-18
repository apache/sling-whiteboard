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

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletException;

import org.apache.sling.extensions.oidc_rp.OidcConnection;
import org.apache.sling.extensions.oidc_rp.impl.OidcEntryPointServlet;
import org.apache.sling.extensions.oidc_rp.impl.OidcProviderMetadataRegistry;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.SubjectType;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

@ExtendWith(SlingContextExtension.class)
class OidcEntryPointServletTest {

    private static final String MOCK_OIDC_PARAM = "mock-oidc-param";
    
    private final SlingContext context = new SlingContext();
    private List<OidcConnection> connections;
    private OidcEntryPointServlet servlet;
    
    @BeforeEach
    void initServlet() {
        connections = Arrays.asList(
                MockOidcConnection.DEFAULT_CONNECTION,
                new MockOidcConnection(new String[] {"openid"}, MOCK_OIDC_PARAM, "client-id", "client-secret", "http://example.com", new String[] { "access_type=offline" } )
            );
        servlet =  new OidcEntryPointServlet(connections, new OidcProviderMetadataRegistry() {
            @Override
            protected OIDCProviderMetadata getProviderMetadata(String base) {
                return new OIDCProviderMetadata(new Issuer(base), Collections.singletonList(SubjectType.PUBLIC), URI.create("https://foo.example/jwks")) {
                    @Override
                    public URI getAuthorizationEndpointURI() {
                        return URI.create("https://foo.example/authz");
                    }
                };
            }
        });
    }
    
    @Test
    void testRedirectWithValidConnection() throws ServletException, IOException {

        context.request().setQueryString("c=" + MockOidcConnection.DEFAULT_CONNECTION.name());
        MockSlingHttpServletResponse response = context.response();
        
        servlet.service(context.request(), response);
        
        URI location = URI.create(Objects.requireNonNull(response.getHeader("Location"), "location header"));
        
        assertThat(location).as("authentication request uri")
            .hasScheme("https")
            .hasHost("foo.example")
            .hasPath("/authz")
            .hasParameter("scope", "openid")
            .hasParameter("response_type", "code")
            .hasParameter("client_id", "client-id")
            .hasParameter("redirect_uri", "http://localhost/system/sling/oidc/callback")
            .hasParameter("nonce")
            .hasParameter("state");        
    }
    
    @Test
    void redirectWithValidConnectionAndCustomParameter() throws ServletException, IOException {
     
        context.request().setQueryString("c=" + MOCK_OIDC_PARAM);
        MockSlingHttpServletResponse response = context.response();
        
        servlet.service(context.request(), response);
        
        URI location = URI.create(Objects.requireNonNull(response.getHeader("Location"), "location header"));
        
        assertThat(location).as("authentication request uri")
            .hasParameter("access_type", "offline");
    }
}
