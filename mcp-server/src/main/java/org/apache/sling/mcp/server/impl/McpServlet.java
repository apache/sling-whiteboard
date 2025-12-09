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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.jackson.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult.CompleteCompletion;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.sling.api.SlingJakartaHttpServletRequest;
import org.apache.sling.api.SlingJakartaHttpServletResponse;
import org.apache.sling.api.servlets.SlingJakartaAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

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

    private static String getStateString(int state) {
        return switch (state) {
            case Bundle.UNINSTALLED -> "UNINSTALLED";
            case Bundle.INSTALLED -> "INSTALLED";
            case Bundle.RESOLVED -> "RESOLVED";
            case Bundle.STARTING -> "STARTING";
            case Bundle.STOPPING -> "STOPPING";
            case Bundle.ACTIVE -> "ACTIVE";
            default -> "UNKNOWN";
        };
    }

    @Activate
    public McpServlet(BundleContext ctx, Config config) throws IllegalAccessException, NoSuchMethodException {

        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());

        transportProvider = HttpServletStatelessServerTransport.builder()
                .messageEndpoint(ENDPOINT)
                .jsonMapper(jsonMapper)
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

        SyncCompletionSpecification servletCompletionSpec = new McpStatelessServerFeatures.SyncCompletionSpecification(
                new McpSchema.PromptReference("ref/prompt", "new-sling-servlet"), (context, request) -> {
                    return new McpSchema.CompleteResult(new CompleteCompletion(List.of(), 0, false));
                });

        String serverVersion = config.serverVersion();
        if (serverVersion == null || serverVersion.isEmpty()) {
            serverVersion = ctx.getBundle().getVersion().toString();
        }
        syncServer = McpServer.sync(transportProvider)
                .serverInfo(config.serverTitle(), serverVersion)
                .jsonMapper(jsonMapper)
                .jsonSchemaValidator(new DefaultJsonSchemaValidator())
                .instructions(config.instructions())
                .completions(List.of(servletCompletionSpec))
                .capabilities(ServerCapabilities.builder()
                        .tools(false)
                        .prompts(false)
                        .resources(false, false)
                        .completions()
                        .build())
                .build();

        var schema = """
                {
                  "type" : "object",
                  "id" : "urn:jsonschema:Operation",
                  "properties" : { }
                }
                """;
        var syncToolSpecification = new SyncToolSpecification(
                Tool.builder()
                        .name("refresh-packages")
                        .description("Refresh Packages")
                        .inputSchema(jsonMapper, schema)
                        .build(),
                (exchange, arguments) -> {
                    FrameworkWiring fw = ctx.getBundle(0).adapt(FrameworkWiring.class);

                    fw.refreshBundles(null);

                    return new CallToolResult("Bundles refreshed successfully", Boolean.FALSE);
                });

        // Register tools, resources, and prompts
        syncServer.addTool(syncToolSpecification);
        syncServer.addPrompt(new SyncPromptSpecification(
                new Prompt(
                        "new-sling-servlet",
                        "Create new Sling Servlet",
                        "Creates a new Sling Servlet in the current project using annotations",
                        List.of(new PromptArgument(
                                "resource-type",
                                "Resource type",
                                "The Sling resource type to bind this servlet to.",
                                true))),
                (context, request) -> {
                    String resourceType = (String) request.arguments().get("resource-type");
                    PromptMessage msg = new PromptMessage(
                            Role.ASSISTANT,
                            new McpSchema.TextContent(
                                    "Create a new Sling Servlet for resource type: " + resourceType
                                            + " . Use the Sling-specific OSGi declarative services annotations - @SlingServletResourceTypes and @Component . Configure by default with the GET method and the json extension. Provide a basic implementation of the doGet method that returns a JSON response with a message 'Hello from Sling Servlet at resource type <resource-type>'."));
                    return new McpSchema.GetPromptResult("Result of creation", List.of(msg));
                }));
        syncServer.addResource(new McpStatelessServerFeatures.SyncResourceSpecification(
                new Resource.Builder()
                        .name("bundle")
                        .uri("bundle://")
                        .description("OSGi bundle status")
                        .mimeType("text/plain")
                        .build(),
                (context, request) -> {
                    String bundleInfo = Stream.of(ctx.getBundles())
                            .map(b -> "Bundle " + b.getSymbolicName() + " is in state " + getStateString(b.getState())
                                    + " (" + b.getState() + ")")
                            .collect(Collectors.joining("\n"));

                    TextResourceContents contents = new TextResourceContents("bundle://", "text/plain", bundleInfo);

                    return new McpSchema.ReadResourceResult(List.of(contents));
                }));

        syncServer.addResourceTemplate(new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                new ResourceTemplate.Builder()
                        .uriTemplate("bundles://state/{state}")
                        .name("bundles")
                        .build(),
                (context, request) -> {
                    String bundleInfo = "";
                    if ("bundles://state/resolved".equals(request.uri().toLowerCase(Locale.ENGLISH))) {
                        bundleInfo = Arrays.stream(ctx.getBundles())
                                .filter(b -> b.getState() == Bundle.RESOLVED)
                                .map(b -> "Bundle " + b.getSymbolicName() + " is in state "
                                        + getStateString(b.getState()) + " (" + b.getState() + ")")
                                .collect(Collectors.joining("\n"));
                    }

                    TextResourceContents contents = new TextResourceContents(request.uri(), "text/plain", bundleInfo);

                    return new ReadResourceResult(List.of(contents));
                }));
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
