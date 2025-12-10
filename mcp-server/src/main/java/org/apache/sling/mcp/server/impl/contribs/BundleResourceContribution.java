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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.apache.sling.mcp.server.impl.McpServerContribution;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component
public class BundleResourceContribution implements McpServerContribution {

    private static final String URI_BUNDLES_ALL = "bundles://all";
    private static final String RESOURCE_TEMPLATE_BUNDLES_STATE_PREFIX = "bundles://state/";
    private static final String RESOURCE_TEMPLATE_BUNDLES_STATE_PATTERN =
            RESOURCE_TEMPLATE_BUNDLES_STATE_PREFIX + "{state}";

    private BundleContext ctx;

    @Activate
    public BundleResourceContribution(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Optional<SyncResourceSpecification> getSyncResourceSpecification() {

        return Optional.of(new McpStatelessServerFeatures.SyncResourceSpecification(
                new Resource.Builder()
                        .name("bundles")
                        .uri(URI_BUNDLES_ALL)
                        .description(
                                "List all OSGi bundles with symbolic name, version, and state. Fragment bundles are marked with [Fragment]")
                        .mimeType("text/plain")
                        .build(),
                (context, request) -> {
                    String bundleInfo =
                            Stream.of(ctx.getBundles()).map(this::describe).collect(Collectors.joining("\n"));

                    TextResourceContents contents = new TextResourceContents(URI_BUNDLES_ALL, "text/plain", bundleInfo);

                    return new McpSchema.ReadResourceResult(List.of(contents));
                }));
    }

    @Override
    public Optional<SyncResourceTemplateSpecification> getSyncResourceTemplateSpecification() {
        return Optional.of(new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                new ResourceTemplate.Builder()
                        .uriTemplate(RESOURCE_TEMPLATE_BUNDLES_STATE_PATTERN)
                        .name("bundles")
                        .build(),
                (context, request) -> {
                    String requestedState = request.uri().substring(RESOURCE_TEMPLATE_BUNDLES_STATE_PREFIX.length());
                    try {
                        BundleState bundleState = BundleState.valueOf(requestedState.toUpperCase(Locale.ENGLISH));
                        if (!bundleState.isValid()) {
                            throw new IllegalArgumentException("Invalid bundle state: " + requestedState);
                        }
                        String bundleInfo = Arrays.stream(ctx.getBundles())
                                .filter(b -> b.getState() == bundleState.getState())
                                .map(this::describe)
                                .collect(Collectors.joining("\n"));

                        TextResourceContents contents =
                                new TextResourceContents(request.uri(), "text/plain", bundleInfo);

                        return new ReadResourceResult(List.of(contents));
                    } catch (IllegalArgumentException e) {
                        return new ReadResourceResult(List.of(new TextResourceContents(
                                request.uri(), "text/plain", "Invalid bundle state requested: " + requestedState)));
                    }
                }));
    }

    @Override
    public Optional<SyncCompletionSpecification> getSyncCompletionSpecification() {

        return Optional.of(new McpStatelessServerFeatures.SyncCompletionSpecification(
                new McpSchema.ResourceReference("ref/resource", RESOURCE_TEMPLATE_BUNDLES_STATE_PATTERN),
                (context, request) -> {

                    // expect argument name to always be "state"
                    String requestedState = request.argument().value();
                    List<String> states = Stream.of(BundleState.values())
                            .filter(BundleState::isValid)
                            .map(s -> s.name().toLowerCase(Locale.ENGLISH))
                            .toList();
                    if (requestedState != null && !requestedState.isEmpty()) {
                        states = states.stream()
                                .filter(s -> s.startsWith(requestedState.toLowerCase(Locale.ENGLISH)))
                                .toList();
                    }
                    return new McpSchema.CompleteResult(
                            new McpSchema.CompleteResult.CompleteCompletion(states, states.size(), false));
                }));
    }

    private String describe(Bundle b) {
        boolean isFragment = Optional.ofNullable(b.adapt(BundleRevision.class)).stream()
                .map(br -> (br.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0)
                .findAny()
                .orElse(false);
        String additionalInfo = isFragment ? " [Fragment]" : "";
        return "Bundle " + b.getSymbolicName() + additionalInfo + " (version " + b.getVersion() + ") is in state "
                + BundleState.fromState(b.getState()) + " (" + b.getState() + ")";
    }
}
