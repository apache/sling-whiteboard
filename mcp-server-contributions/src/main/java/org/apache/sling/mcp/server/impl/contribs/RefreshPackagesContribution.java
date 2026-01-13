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

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.apache.sling.mcp.server.spi.McpServerContribution;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class RefreshPackagesContribution implements McpServerContribution {

    @Reference
    private McpJsonMapper jsonMapper;

    private final BundleContext ctx;

    @Activate
    public RefreshPackagesContribution(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<SyncToolSpecification> getSyncToolSpecification() {

        var schema = """
                {
                  "type" : "object",
                  "id" : "urn:jsonschema:Operation",
                  "properties" : { }
                }
                """;

        return List.of(new SyncToolSpecification(
                Tool.builder()
                        .name("refresh-packages")
                        .description("Refresh Packages")
                        .inputSchema(jsonMapper, schema)
                        .build(),
                (exchange, arguments) -> {
                    FrameworkWiring fw = ctx.getBundle(0).adapt(FrameworkWiring.class);

                    fw.refreshBundles(null);

                    return CallToolResult.builder()
                            .addTextContent("Bundles refreshed successfully")
                            .build();
                }));
    }
}
