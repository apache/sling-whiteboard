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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.apache.sling.mcp.server.impl.McpServerContribution;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

/**
 * MCP Tool that diagnoses why OSGi bundles and components aren't starting.
 * This tool provides detailed diagnostic information about:
 * - Bundle state and resolution issues
 * - Unsatisfied package imports
 * - Missing service dependencies for components
 * - Configuration problems
 */
@Component
public class OsgiBundleDiagnosticContribution implements McpServerContribution {

    @Reference
    private McpJsonMapper jsonMapper;

    @Reference
    private ServiceComponentRuntime scr;

    private final BundleContext ctx;

    @Activate
    public OsgiBundleDiagnosticContribution(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Optional<SyncToolSpecification> getSyncToolSpecification() {

        var schema = """
                {
                  "type" : "object",
                  "id" : "urn:jsonschema:DiagnoseBundleInput",
                  "properties" : {
                    "bundleSymbolicName" : {
                      "type" : "string",
                      "description" : "The symbolic name of the bundle to diagnose. If not provided, will diagnose all problematic bundles."
                    }
                  }
                }
                """;

        return Optional.of(new SyncToolSpecification(
                Tool.builder()
                        .name("diagnose-osgi-bundle")
                        .description(
                                "Diagnose why an OSGi bundle or component isn't starting. Provides detailed information about unsatisfied dependencies, missing packages, and component configuration issues.")
                        .inputSchema(jsonMapper, schema)
                        .build(),
                (exchange, request) -> {
                    String bundleSymbolicName = (String) request.arguments().get("bundleSymbolicName");

                    if (bundleSymbolicName != null && !bundleSymbolicName.isEmpty()) {
                        return diagnoseSpecificBundle(bundleSymbolicName);
                    } else {
                        return diagnoseAllProblematicBundles();
                    }
                }));
    }

    private CallToolResult diagnoseSpecificBundle(String symbolicName) {
        Bundle bundle = findBundle(symbolicName);
        if (bundle == null) {
            return new CallToolResult("Bundle '" + symbolicName + "' not found.", Boolean.TRUE);
        }

        StringBuilder result = new StringBuilder();
        result.append("=== Bundle Diagnostic Report ===\n\n");
        result.append("Bundle: ").append(bundle.getSymbolicName()).append("\n");
        result.append("Version: ").append(bundle.getVersion()).append("\n");
        result.append("State: ").append(getStateName(bundle.getState())).append("\n\n");

        if (bundle.getState() == Bundle.ACTIVE) {
            result.append("✓ Bundle is ACTIVE and running normally.\n\n");
            // Check components
            appendComponentDiagnostics(bundle, result);
        } else {
            result.append("✗ Bundle is NOT active. Analyzing issues...\n\n");
            analyzeBundleIssues(bundle, result);
            appendComponentDiagnostics(bundle, result);
        }

        return new CallToolResult(result.toString(), Boolean.FALSE);
    }

    private CallToolResult diagnoseAllProblematicBundles() {
        StringBuilder result = new StringBuilder();
        result.append("=== OSGi System Diagnostic Report ===\n\n");

        List<Bundle> problematicBundles = Arrays.stream(ctx.getBundles())
                .filter(b -> b.getState() != Bundle.ACTIVE && b.getState() != Bundle.UNINSTALLED)
                .collect(Collectors.toList());

        if (problematicBundles.isEmpty()) {
            result.append("✓ All bundles are active!\n\n");
        } else {
            result.append("Found ").append(problematicBundles.size()).append(" problematic bundle(s):\n\n");

            for (Bundle bundle : problematicBundles) {
                result.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                result.append("Bundle: ").append(bundle.getSymbolicName()).append("\n");
                result.append("State: ").append(getStateName(bundle.getState())).append("\n");
                analyzeBundleIssues(bundle, result);
                result.append("\n");
            }
        }

        // Check for components with issues
        appendAllComponentIssues(result);

        return new CallToolResult(result.toString(), Boolean.FALSE);
    }

    private void analyzeBundleIssues(Bundle bundle, StringBuilder result) {
        if (bundle.getState() == Bundle.INSTALLED) {
            result.append("\n⚠ Bundle is INSTALLED but not RESOLVED\n");
            result.append("This typically means there are unsatisfied dependencies.\n\n");

            BundleWiring wiring = bundle.adapt(BundleWiring.class);
            if (wiring != null) {
                List<BundleRequirement> requirements = wiring.getRequirements(null);
                boolean foundIssues = false;

                for (BundleRequirement req : requirements) {
                    List<BundleWire> wires = wiring.getRequiredWires(req.getNamespace());
                    if (wires == null || wires.isEmpty()) {
                        if (!foundIssues) {
                            result.append("Unsatisfied Requirements:\n");
                            foundIssues = true;
                        }
                        result.append("  ✗ ")
                                .append(req.getNamespace())
                                .append(": ")
                                .append(req.getDirectives())
                                .append("\n");

                        // For package imports, show which package is missing
                        if ("osgi.wiring.package".equals(req.getNamespace())) {
                            String filter = req.getDirectives().get("filter");
                            result.append("    Missing package: ")
                                    .append(filter)
                                    .append("\n");
                        }
                    }
                }

                if (!foundIssues) {
                    result.append("No obvious dependency issues found. Bundle may have internal errors.\n");
                }
            }
        } else if (bundle.getState() == Bundle.RESOLVED) {
            result.append("\n✓ Bundle is RESOLVED (all dependencies satisfied)\n");
            result.append("Bundle can be started manually if it's not a fragment.\n");
        } else if (bundle.getState() == Bundle.STARTING) {
            result.append("\n⚠ Bundle is STARTING (stuck during activation)\n");
            result.append("Check for errors in bundle activator or circular dependencies.\n");
        }

        // Check for fragment information
        if ((bundle.getHeaders().get(Constants.FRAGMENT_HOST)) != null) {
            result.append("\nNote: This is a fragment bundle (attached to: ")
                    .append(bundle.getHeaders().get(Constants.FRAGMENT_HOST))
                    .append(")\n");
        }
    }

    private void appendComponentDiagnostics(Bundle bundle, StringBuilder result) {
        Collection<ComponentDescriptionDTO> components = scr.getComponentDescriptionDTOs(bundle);

        if (components.isEmpty()) {
            return;
        }

        result.append("\n--- Declarative Services Components ---\n\n");

        for (ComponentDescriptionDTO desc : components) {
            Collection<ComponentConfigurationDTO> configs = scr.getComponentConfigurationDTOs(desc);

            result.append("Component: ").append(desc.name).append("\n");

            if (configs.isEmpty()) {
                result.append("  Status: Not configured/instantiated\n");
            }

            for (ComponentConfigurationDTO config : configs) {
                result.append("  State: ")
                        .append(getComponentStateName(config.state))
                        .append("\n");

                if (config.state == ComponentConfigurationDTO.UNSATISFIED_REFERENCE) {
                    result.append("  ✗ Unsatisfied Service References:\n");
                    for (UnsatisfiedReferenceDTO ref : config.unsatisfiedReferences) {
                        result.append("    - ")
                                .append(ref.name)
                                .append(" (")
                                .append(ref.target)
                                .append(")\n");
                    }
                } else if (config.state == ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION) {
                    result.append("  ✗ Missing required configuration\n");
                } else if (config.state == ComponentConfigurationDTO.SATISFIED
                        || config.state == ComponentConfigurationDTO.ACTIVE) {
                    result.append("  ✓ Component is working correctly\n");
                }
            }
            result.append("\n");
        }
    }

    private void appendAllComponentIssues(StringBuilder result) {
        List<ComponentDescriptionDTO> allComponents = new ArrayList<>(scr.getComponentDescriptionDTOs());
        List<String> problematicComponents = new ArrayList<>();

        for (ComponentDescriptionDTO desc : allComponents) {
            Collection<ComponentConfigurationDTO> configs = scr.getComponentConfigurationDTOs(desc);

            for (ComponentConfigurationDTO config : configs) {
                if (config.state == ComponentConfigurationDTO.UNSATISFIED_REFERENCE
                        || config.state == ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION) {
                    problematicComponents.add(desc.name + " (" + getComponentStateName(config.state) + ")");
                }
            }
        }

        if (!problematicComponents.isEmpty()) {
            result.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            result.append("Problematic Components:\n\n");
            for (String comp : problematicComponents) {
                result.append("  ✗ ").append(comp).append("\n");
            }
        }
    }

    private Bundle findBundle(String symbolicName) {
        return Arrays.stream(ctx.getBundles())
                .filter(b -> b.getSymbolicName().equals(symbolicName))
                .findFirst()
                .orElse(null);
    }

    private String getStateName(int state) {
        return switch (state) {
            case Bundle.UNINSTALLED -> "UNINSTALLED";
            case Bundle.INSTALLED -> "INSTALLED";
            case Bundle.RESOLVED -> "RESOLVED";
            case Bundle.STARTING -> "STARTING";
            case Bundle.STOPPING -> "STOPPING";
            case Bundle.ACTIVE -> "ACTIVE";
            default -> "UNKNOWN (" + state + ")";
        };
    }

    private String getComponentStateName(int state) {
        return switch (state) {
            case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION -> "UNSATISFIED_CONFIGURATION";
            case ComponentConfigurationDTO.UNSATISFIED_REFERENCE -> "UNSATISFIED_REFERENCE";
            case ComponentConfigurationDTO.SATISFIED -> "SATISFIED";
            case ComponentConfigurationDTO.ACTIVE -> "ACTIVE";
            default -> "UNKNOWN (" + state + ")";
        };
    }
}
