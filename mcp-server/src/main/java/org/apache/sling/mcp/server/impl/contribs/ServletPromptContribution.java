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
package org.apache.sling.mcp.server.impl.contribs;

import java.util.List;
import java.util.Optional;

import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult.CompleteCompletion;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import org.apache.sling.mcp.server.spi.McpServerContribution;
import org.osgi.service.component.annotations.Component;

@Component
public class ServletPromptContribution implements McpServerContribution {

    @Override
    public Optional<SyncPromptSpecification> getSyncPromptSpecification() {
        return Optional.of(new SyncPromptSpecification(
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
                                    "Create a new Sling Servlet for resource type: "
                                            + resourceType
                                            + " . Use the Sling-specific OSGi declarative services annotations - @SlingServletResourceTypes and @Component . Configure by default with the GET method and the json extension. Provide a basic implementation of the doGet method that returns a JSON response with a message 'Hello from Sling Servlet at resource type <resource-type>'."));
                    return new McpSchema.GetPromptResult("Result of creation", List.of(msg));
                }));
    }

    @Override
    public Optional<SyncCompletionSpecification> getSyncCompletionSpecification() {
        // supply no completions for various resource types because it's supposed to be specified by the user
        return Optional.of(new McpStatelessServerFeatures.SyncCompletionSpecification(
                new McpSchema.PromptReference("ref/prompt", "new-sling-servlet"), (context, request) -> {
                    return new McpSchema.CompleteResult(new CompleteCompletion(List.of(), 0, false));
                }));
    }
}
