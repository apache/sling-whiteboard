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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.auth.oauth_client.ClientConnection;
import org.apache.sling.auth.oauth_client.impl.OAuthEntryPointServlet;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SlingContextExtension.class)
class OAuthEntryPointServletTest {

    private static final String MOCK_OIDC_PARAM = "mock-oidc-param";
    
    private final SlingContext context = new SlingContext();
    private List<ClientConnection> connections;
    private OAuthEntryPointServlet servlet;
    
    @BeforeEach
    void initServlet() {
        connections = Arrays.asList(
                MockOidcConnection.DEFAULT_CONNECTION,
                new MockOidcConnection(new String[] {"openid"}, MOCK_OIDC_PARAM, "client-id", "client-secret", "http://example.com", new String[] { "access_type=offline" } )
            );
        servlet =  new OAuthEntryPointServlet(connections, new StubOAuthStateManager());
    }
    
    @Test
    void testRedirectWithValidConnection() throws ServletException, IOException {

        context.request().setQueryString("c=" + MockOidcConnection.DEFAULT_CONNECTION.name());
        MockSlingHttpServletResponse response = context.response();
        
        servlet.service(context.request(), response);
        
        URI location = URI.create(Objects.requireNonNull(response.getHeader("Location"), "location header"));
        
        assertThat(location).as("authentication request uri")
            .hasScheme("https")
            .hasHost("example.com")
            .hasPath("/authorize")
            .hasParameter("scope", "openid")
            .hasParameter("response_type", "code")
            .hasParameter("client_id", "client-id")
            .hasParameter("redirect_uri", "http://localhost/system/sling/oauth/callback")
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
    
    @Test
    void missingConnectionParameter() throws ServletException, IOException {
        
        servlet.service(context.request(), context.response());
        
        assertThat(context.response().getStatus()).as("response code")
            .isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }
    
    @Test
    void invalidConnectionParameter() throws ServletException, IOException {
        
        context.request().setQueryString("c=invalid");
        
        MockSlingHttpServletResponse response = context.response();
        servlet.service(context.request(), response);
        
        assertThat(context.response().getStatus()).as("response code")
            .isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }
}
