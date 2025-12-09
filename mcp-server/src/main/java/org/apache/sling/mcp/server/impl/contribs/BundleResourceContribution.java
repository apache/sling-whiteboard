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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component
public class BundleResourceContribution implements McpServerContribution {

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

    private BundleContext ctx;

    @Activate
    public BundleResourceContribution(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Optional<SyncResourceSpecification> getSyncResourceSpecification() {

        return Optional.of(new McpStatelessServerFeatures.SyncResourceSpecification(
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
    }

    @Override
    public Optional<SyncResourceTemplateSpecification> getSyncResourceTemplateSpecification() {
        return Optional.of(new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
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
}
