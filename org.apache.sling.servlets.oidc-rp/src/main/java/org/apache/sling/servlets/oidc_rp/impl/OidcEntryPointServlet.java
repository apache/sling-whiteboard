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

import static org.apache.sling.servlets.oidc_rp.impl.OidcStateManager.PARAMETER_NAME_CONNECTION;
import static org.apache.sling.servlets.oidc_rp.impl.OidcStateManager.PARAMETER_NAME_REDIRECT;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.apache.sling.servlets.oidc_rp.OidcConnection;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

@Component(service = { Servlet.class })
@SlingServletPaths(OidcEntryPointServlet.PATH)
public class OidcEntryPointServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1L;

    static final String PATH = "/system/sling/oidc/entry-point"; // NOSONAR
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final Map<String, OidcConnection> connections;

    private final OidcProviderMetadataRegistry metadataRegistry;

    @Activate
    public OidcEntryPointServlet(@Reference(policyOption = GREEDY) List<OidcConnection> connections,
            @Reference OidcProviderMetadataRegistry metadataRegistry) {
        this.connections = connections.stream()
                .collect(Collectors.toMap( OidcConnection::name, Function.identity()));
        this.metadataRegistry = metadataRegistry;
    }
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        
        String desiredConnectionName = request.getParameter("c");
        if ( desiredConnectionName == null ) {
            logger.debug("Missing mandatory request parameter 'c'");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        OidcConnection connection = connections.get(desiredConnectionName);
        if ( connection == null ) {
            logger.debug("Client requested unknown connection {}; known: {}", desiredConnectionName, connections.keySet());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        if ( connection.baseUrl() == null )
            throw new ServletException("Misconfigured baseUrl");
        try {

            OIDCProviderMetadata providerMetadata = metadataRegistry.getProviderMetadata(connection.baseUrl());

            // The client ID provisioned by the OpenID provider when
            // the client was registered
            ClientID clientID = new ClientID(connection.clientId());

            // The client callback URL
            URI callback = new URI(OidcCallbackServlet.getCallbackUri(request));
            // Generate random state string to securely pair the callback to this request
            State state = new State();
            OidcStateManager stateManager = OidcStateManager.stateFor(request);
            stateManager.registerState(state);
            stateManager.putAttribute(state, PARAMETER_NAME_CONNECTION, desiredConnectionName);
            if ( request.getParameter(PARAMETER_NAME_REDIRECT) != null )
                stateManager.putAttribute(state, PARAMETER_NAME_REDIRECT, request.getParameter(PARAMETER_NAME_REDIRECT));

            // Generate nonce for the ID token
            Nonce nonce = new Nonce();

            // Compose the OpenID authentication request (for the code flow)
            AuthenticationRequest authRequest = new AuthenticationRequest.Builder(
                    new ResponseType("code"),
                    new Scope(connection.scopes()),
                    clientID, callback)
                .endpointURI(providerMetadata.getAuthorizationEndpointURI())
                .state(state)
                .nonce(nonce)
                .customParameter("access_type", "offline") // request refresh token. TODO - is this Google-specific?
                .build();

            response.sendRedirect(authRequest.toURI().toString());
        } catch (URISyntaxException e) {
            throw new ServletException(e);
        }
    }
}
