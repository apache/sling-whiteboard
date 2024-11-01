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

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.extensions.oauth_client.ClientConnection;
import org.apache.sling.extensions.oauth_client.OAuthTokenStore;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SlingContextExtension.class)
class OAuthCallbackServletTest {
    
    private static final String MOCK_OIDC_PARAM = "mock-oidc-param";

    private final SlingContext context = new SlingContext();
    
    private List<ClientConnection> connections;
    
    @BeforeEach
    void initConnections() {
        connections = Arrays.asList(
                MockOidcConnection.DEFAULT_CONNECTION,
                new MockOidcConnection(new String[] {"openid"}, MOCK_OIDC_PARAM, "client-id", "client-secret", "http://example.com", new String[] { "access_type=offline" } )
            );
    }

    @Test
    void missingConnectionParameter() throws ServletException, IOException {
        
        OAuthTokenStore tokenStore = null;

        OAuthCallbackServlet servlet = new OAuthCallbackServlet(connections, tokenStore, new StubOAuthStateManager());
        
        servlet.service(context.request(), context.response());
        
        assertThat(context.response().getStatus()).as("response code")
            .isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void malformedStateParameter() throws ServletException, IOException {

        OAuthTokenStore tokenStore = null;
        
        OAuthCallbackServlet servlet = new OAuthCallbackServlet(connections, tokenStore, new StubOAuthStateManager());
        
        context.request().setQueryString("?code=foo&state=bar");
        
        servlet.service(context.request(), context.response());
        
        assertThat(context.response().getStatus()).as("response code")
            .isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }
    
    @Test
    void missingStateCookie() throws ServletException, IOException {

        OAuthTokenStore tokenStore = null;
        
        OAuthCallbackServlet servlet = new OAuthCallbackServlet(connections, tokenStore, new StubOAuthStateManager());
        
        String state = URLEncoder.encode(format("%s|bar", MockOidcConnection.DEFAULT_CONNECTION.name()), StandardCharsets.UTF_8);
        
        context.request().setQueryString(format("code=foo&state=%s", state));
        
        servlet.service(context.request(), context.response());
        
        assertThat(context.response().getStatus()).as("response code")
            .isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    }
    
    @Test
    void invalidStateCookie() {
        OAuthTokenStore tokenStore = null;
        
        OAuthCallbackServlet servlet = new OAuthCallbackServlet(connections, tokenStore, new StubOAuthStateManager());
        
        String state = URLEncoder.encode(format("bar|%s", MockOidcConnection.DEFAULT_CONNECTION.name()), StandardCharsets.UTF_8);
        
        context.request().setQueryString(format("code=foo&state=%s", state));
        context.request().addCookie(new Cookie(OAuthStateManager.COOKIE_NAME_REQUEST_KEY, "baz"));
        
        OAuthCallbackException thrown = assertThrowsExactly(OAuthCallbackException.class, () -> {
            servlet.service(context.request(), context.response());
        });
        assertThat(thrown).hasMessage("State check failed");
    }
    
    @Test
    void errorCode() {
        OAuthTokenStore tokenStore = null;
        
        OAuthCallbackServlet servlet = new OAuthCallbackServlet(connections, tokenStore, new StubOAuthStateManager());
        
        String state = URLEncoder.encode(format("bar|%s", MockOidcConnection.DEFAULT_CONNECTION.name()), StandardCharsets.UTF_8);
        
        context.request().setQueryString(format("error=access_denied&state=%s", state));
        context.request().addCookie(new Cookie(OAuthStateManager.COOKIE_NAME_REQUEST_KEY, "bar"));
        
        OAuthCallbackException thrown = assertThrowsExactly(OAuthCallbackException.class, () -> {
            servlet.service(context.request(), context.response());
        });
        assertThat(thrown).hasMessage("Authentication failed");
    }
}
