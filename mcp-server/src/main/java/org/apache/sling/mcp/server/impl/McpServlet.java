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
package org.apache.sling.mcp.server.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.jackson.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

@Component(service = Servlet.class)
@SlingServletPaths(value = {McpServlet.ENDPOINT})
@Designate(ocd = McpServlet.Config.class)
public class McpServlet extends SlingJakartaAllMethodsServlet {

    @ObjectClassDefinition(name = "Apache Sling MCP Server Configuration")
    public @interface Config {
        @AttributeDefinition(name = "Server Title", description = "The title of the MCP server")
        String serverTitle() default "Apache Sling";

        @AttributeDefinition(
                name = "Server Version",
                description = "The version of the MCP server. Defaults to the bundle version if not set")
        String serverVersion();

        @AttributeDefinition(name = "Instructions", description = "Initial instructions for the MCP server")
        String instructions() default
                "This MCP server provides access to an Apache Sling development instance. Exposed tools and information always reference the Sling deployment and not local projects or files";
    }

    static final String ENDPOINT = "/bin/mcp";
    private static final long serialVersionUID = 1L;
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private McpStatelessSyncServer syncServer;
    private HttpServletStatelessServerTransport transportProvider;
    private MethodHandle doGetMethod;
    private MethodHandle doPostMethod;

    @Activate
    public McpServlet(
            BundleContext ctx,
            Config config,
            @Reference McpJsonMapper jsonMapper,
            @Reference(cardinality = MULTIPLE, policyOption = GREEDY) List<McpServerContribution> contributions)
            throws IllegalAccessException, NoSuchMethodException {

        transportProvider = HttpServletStatelessServerTransport.builder()
                .messageEndpoint(ENDPOINT)
                .jsonMapper(jsonMapper)
                .contextExtractor(request -> McpTransportContext.create(
                        Map.of("resourceResolver", ((SlingJakartaHttpServletRequest) request).getResourceResolver())))
                .build();

        MethodHandles.Lookup privateLookup =
                MethodHandles.privateLookupIn(HttpServletStatelessServerTransport.class, LOOKUP);

        doGetMethod = privateLookup.findVirtual(
                HttpServletStatelessServerTransport.class,
                "doGet",
                java.lang.invoke.MethodType.methodType(
                        void.class, HttpServletRequest.class, HttpServletResponse.class));
        doPostMethod = privateLookup.findVirtual(
                HttpServletStatelessServerTransport.class,
                "doPost",
                java.lang.invoke.MethodType.methodType(
                        void.class, HttpServletRequest.class, HttpServletResponse.class));

        String serverVersion = config.serverVersion();
        if (serverVersion == null || serverVersion.isEmpty()) {
            serverVersion = ctx.getBundle().getVersion().toString();
        }

        var completions = contributions.stream()
                .map(McpServerContribution::getSyncCompletionSpecification)
                .flatMap(Optional::stream)
                .toList();

        syncServer = McpServer.sync(transportProvider)
                .serverInfo(config.serverTitle(), serverVersion)
                .jsonMapper(jsonMapper)
                .jsonSchemaValidator(new DefaultJsonSchemaValidator())
                .instructions(config.instructions())
                .completions(completions)
                .capabilities(ServerCapabilities.builder()
                        .tools(false)
                        .prompts(false)
                        .resources(false, false)
                        .completions()
                        .build())
                .build();

        contributions.stream()
                .map(McpServerContribution::getSyncToolSpecification)
                .flatMap(Optional::stream)
                .forEach(syncTool -> syncServer.addTool(syncTool));

        contributions.stream()
                .map(McpServerContribution::getSyncResourceSpecification)
                .flatMap(Optional::stream)
                .forEach(syncResource -> syncServer.addResource(syncResource));

        contributions.stream()
                .map(McpServerContribution::getSyncResourceTemplateSpecification)
                .flatMap(Optional::stream)
                .forEach(syncResource -> syncServer.addResourceTemplate(syncResource));

        contributions.stream()
                .map(McpServerContribution::getSyncPromptSpecification)
                .flatMap(Optional::stream)
                .forEach(syncPrompt -> syncServer.addPrompt(syncPrompt));
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE)
    protected void bindPrompt(DiscoveredPrompt prompt, Map<String, Object> properties) {
        syncServer.addPrompt(new SyncPromptSpecification(prompt.asPrompt(), (c, r) -> {
            var messages = prompt.getPromptMessages(c, r);
            return new McpSchema.GetPromptResult(null, messages);
        }));
    }

    protected void unbindPrompt(Map<String, Object> properties) {
        String promptName = (String) properties.get(DiscoveredPrompt.SERVICE_PROP_NAME);
        syncServer.removePrompt(promptName);
    }

    @Override
    protected void doGet(
            @NotNull SlingJakartaHttpServletRequest request, @NotNull SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {
        try {
            doGetMethod.invoke(transportProvider, request, response);
        } catch (ServletException | IOException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new ServletException(t);
        }
    }

    @Override
    protected void doPost(
            @NotNull SlingJakartaHttpServletRequest request, @NotNull SlingJakartaHttpServletResponse response)
            throws ServletException, IOException {
        try {
            doPostMethod.invoke(transportProvider, request, response);
        } catch (ServletException | IOException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new ServletException(t);
        }
    }

    @Deactivate
    public void close() {
        if (syncServer != null) {
            syncServer.close();
        }
    }
}
