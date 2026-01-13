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

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.apache.sling.mcp.server.spi.McpServerContribution;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

@Component
public class ComponentResourceContribution implements McpServerContribution {

    private static String getStateString(int state) {
        return switch (state) {
            case 1 -> "UNSATISFIED_CONFIGURATION";
            case 2 -> "UNSATISFIED_REFERENCE";
            case 4 -> "SATISFIED";
            case 8 -> "ACTIVE";
            case 16 -> "REGISTERED";
            case 32 -> "FACTORY";
            case 64 -> "DISABLED";
            case 128 -> "ENABLING";
            case 256 -> "ENABLED";
            case 512 -> "DISABLING";
            default -> "UNKNOWN";
        };
    }

    @Reference
    private ServiceComponentRuntime scr;

    @Activate
    public ComponentResourceContribution() {}

    @Override
    public Optional<SyncResourceSpecification> getSyncResourceSpecification() {

        return Optional.of(new McpStatelessServerFeatures.SyncResourceSpecification(
                new Resource.Builder()
                        .name("component")
                        .uri("component://")
                        .description("OSGi component status")
                        .mimeType("text/plain")
                        .build(),
                (context, request) -> {
                    Collection<ComponentDescriptionDTO> components = scr.getComponentDescriptionDTOs();
                    String componentInfo = components.stream()
                            .map(c -> {
                                String state = scr.getComponentConfigurationDTOs(c).stream()
                                        .map(config -> getStateString(config.state))
                                        .collect(Collectors.joining(", "));
                                return "Component " + c.name + " is in state(s): " + state;
                            })
                            .collect(Collectors.joining("\n"));

                    TextResourceContents contents =
                            new TextResourceContents("component://", "text/plain", componentInfo);

                    return new McpSchema.ReadResourceResult(List.of(contents));
                }));
    }

    @Override
    public Optional<SyncResourceTemplateSpecification> getSyncResourceTemplateSpecification() {
        return Optional.of(new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                new ResourceTemplate.Builder()
                        .uriTemplate("components://state/{state}")
                        .name("components")
                        .build(),
                (context, request) -> {
                    String componentInfo = "";
                    String uri = request.uri().toLowerCase(Locale.ENGLISH);

                    if (uri.startsWith("components://state/")) {
                        String requestedState = uri.substring("components://state/".length());
                        Collection<ComponentDescriptionDTO> components = scr.getComponentDescriptionDTOs();

                        componentInfo = components.stream()
                                .flatMap(c -> scr.getComponentConfigurationDTOs(c).stream()
                                        .filter(config -> getStateString(config.state)
                                                .toLowerCase(Locale.ENGLISH)
                                                .equals(requestedState))
                                        .map(config -> "Component " + c.name + " is in state: "
                                                + getStateString(config.state)))
                                .collect(Collectors.joining("\n"));
                    }

                    TextResourceContents contents =
                            new TextResourceContents(request.uri(), "text/plain", componentInfo);

                    return new ReadResourceResult(List.of(contents));
                }));
    }
}
