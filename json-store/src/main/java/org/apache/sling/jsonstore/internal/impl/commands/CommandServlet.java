/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.jsonstore.internal.impl.commands;

import java.io.IOException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jsonstore.internal.api.Command;
import org.apache.sling.jsonstore.internal.api.JsonStoreConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes={
        JsonStoreConstants.COMMAND_RESOURCE_TYPE
    },
    methods= { "GET", "POST" }
)
public class CommandServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private BundleContext bundleContext;
    
    // TODO all path patterns should be configurable in a central place
    private final static Pattern COMMANDS_PATTERN = Pattern.compile("/content/sites/([^/]+)/commands/([^/]+)/([^/]+)");

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Activate
    public void activate(BundleContext ctx) {
        bundleContext = ctx;
    }

    private void doCommand(SlingHttpServletRequest request, SlingHttpServletResponse response, Function<Command, JsonNode> function) throws IOException {
        final ServiceReference<?> ref = selectCommand(request.getResource());
        if(ref == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Command not found at " + request.getResource().getPath());
        }

        final Command c = (Command)bundleContext.getService(ref);
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), function.apply(c));
        } finally {
            bundleContext.ungetService(ref);
        }
    }

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        doCommand(request, response, (Command c) -> { return c.getInfo(); });
    }

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        // Parse incoming JSON
        JsonNode tmp = null;
        try {
            tmp = objectMapper.readTree(request.getInputStream());
        } catch(Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "JSON parsing failed: " + e);
            return;
        }
        final JsonNode json = tmp;

        doCommand(
            request, 
            response, 
            (Command c) -> { 
                try {
                    return c.execute(json); 
                } catch(IOException ioe) {
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Command failed:" + ioe);
                    } catch(IOException ioe2) {
                        log.info("Error sending error response", ioe2);
                    }
                }
                return null;
            });
    }

    private ServiceReference<?> selectCommand(Resource resource) {
        final Matcher m = COMMANDS_PATTERN.matcher(resource.getPath());
        if(m.matches()) {
            final String namespace = m.group(2);
            final String name = m.group(3);

            final String filter = String.format(
                "(&(%s=%s)(%s=%s))",
                Command.SERVICE_PROP_NAMESPACE, namespace,
                Command.SERVICE_PROP_NAME, name
            );

            try {
                final ServiceReference<?>[] services = bundleContext.getServiceReferences(Command.class.getName(), filter);
                final int nCommands = services == null ? 0 : services.length;
                if(nCommands == 1) {
                    return services[0];
                } else {
                    log.info("Expected 1 command for {}/{}, got {}", namespace, name, nCommands);
                }
            } catch(InvalidSyntaxException ise) {
                throw new RuntimeException("Invalid filter syntax", ise);
            }
        }
        return null;
    }

}