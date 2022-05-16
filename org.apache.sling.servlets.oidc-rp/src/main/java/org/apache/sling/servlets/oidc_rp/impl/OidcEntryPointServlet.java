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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;

@Component(service = { Servlet.class })
@SlingServletPaths(OidcEntryPointServlet.PATH)
public class OidcEntryPointServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1L;

    static final String SESSION_ATTRIBUTE_STATE = OidcEntryPointServlet.class.getName() + ".state";
    static final String SESSION_ATTRIBUTE_REDIRECT = OidcEntryPointServlet.class.getName() + ".redirect";
    static final String PATH = "/system/sling/oidc/entry-point"; // NOSONAR
    private final OidcConnection connection;

    @Activate
    public OidcEntryPointServlet(@Reference OidcConnection connection) {
        this.connection = connection;
    }
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        try {

            Endpoints ep = Endpoints.discover(connection.baseUrl(), HttpClient.newHttpClient());

            // The client ID provisioned by the OpenID provider when
            // the client was registered
            ClientID clientID = new ClientID(connection.clientId());

            // The client callback URL
            URI callback = new URI(OidcCallbackServlet.getCallbackUri(request));
            // Generate random state string to securely pair the callback to this request
            State state = new State();
            request.getSession().setAttribute(SESSION_ATTRIBUTE_STATE, state);
            if ( request.getParameter("redirect") != null )
                request.getSession().setAttribute(SESSION_ATTRIBUTE_REDIRECT, request.getParameter("redirect"));

            // Generate nonce for the ID token
            Nonce nonce = new Nonce();

            // Compose the OpenID authentication request (for the code flow)
            AuthenticationRequest authRequest = new AuthenticationRequest.Builder(new ResponseType("code"), new Scope(connection.scopes()),
                    clientID, callback).endpointURI(new URI(ep.authorizationEndpoint())).state(state).nonce(nonce).build();

            response.sendRedirect(authRequest.toURI().toString());
        } catch (URISyntaxException e) {
            throw new ServletException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
