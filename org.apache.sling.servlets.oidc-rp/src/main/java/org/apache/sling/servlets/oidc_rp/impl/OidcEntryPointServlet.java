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

import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.IOException;
import java.net.URI;
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
import org.apache.sling.auth.core.AuthConstants;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.apache.sling.servlets.oidc_rp.OidcClient;
import org.apache.sling.servlets.oidc_rp.OidcConnection;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { Servlet.class },
    property = { AuthConstants.AUTH_REQUIREMENTS +"=" + OidcEntryPointServlet.PATH }
)
@SlingServletPaths(OidcEntryPointServlet.PATH)
public class OidcEntryPointServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1L;

    static final String PATH = "/system/sling/oidc/entry-point"; // NOSONAR
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final Map<String, OidcConnection> connections;

    private final OidcClient oidcClient;

    @Activate
    public OidcEntryPointServlet(@Reference(policyOption = GREEDY) List<OidcConnection> connections,
            @Reference OidcClient oidcClient) {
        this.connections = connections.stream()
                .collect(Collectors.toMap( OidcConnection::name, Function.identity()));
        this.oidcClient = oidcClient;
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
            
        response.sendRedirect(oidcClient.getAuthenticationRequestUri(connection, request, URI.create(OidcCallbackServlet.getCallbackUri(request))).toString());
    }
}
