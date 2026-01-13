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
import java.util.stream.Collectors;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.apache.sling.mcp.server.spi.McpServerContribution;
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
    public List<SyncToolSpecification> getSyncToolSpecification() {

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

        return List.of(new SyncToolSpecification(
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
            return CallToolResult.builder()
                    .addTextContent("Bundle '" + symbolicName + "' not found.")
                    .isError(true)
                    .build();
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

        return CallToolResult.builder().addTextContent(result.toString()).build();
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

        return CallToolResult.builder().addTextContent(result.toString()).build();
    }

    private void analyzeBundleIssues(Bundle bundle, StringBuilder result) {
        if (bundle.getState() == Bundle.INSTALLED) {
            result.append("\n⚠ Bundle is INSTALLED but not RESOLVED\n");
            result.append("This typically means there are unsatisfied dependencies.\n\n");

            boolean foundIssues = false;

            // First try to get info from BundleWiring (for resolved requirements)
            BundleWiring wiring = bundle.adapt(BundleWiring.class);
            if (wiring != null) {
                List<BundleRequirement> requirements = wiring.getRequirements(null);

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
            }

            // If no issues found via wiring, check manifest directly and compare with available exports
            if (!foundIssues) {
                String importPackage = bundle.getHeaders().get(Constants.IMPORT_PACKAGE);

                if (importPackage != null && !importPackage.isEmpty()) {
                    result.append("Analyzing Package Dependencies:\n\n");
                    analyzePackageImports(importPackage, result);
                    foundIssues = true;
                }

                String requireBundle = bundle.getHeaders().get(Constants.REQUIRE_BUNDLE);
                if (requireBundle != null && !requireBundle.isEmpty()) {
                    result.append("\nRequired Bundles (from manifest):\n");
                    result.append("  ")
                            .append(requireBundle.replace(",", ",\n  "))
                            .append("\n\n");
                    result.append("⚠ One or more of these bundles are not available or not in the correct state.\n");
                    foundIssues = true;
                }
            }

            if (!foundIssues) {
                result.append("No dependency information found. Bundle may have internal errors.\n");
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

    /**
     * Analyzes imported packages by checking if they are available in the OSGi environment.
     * Scans all bundles to find which packages are exported and matches them against imports.
     */
    private void analyzePackageImports(String importPackageHeader, StringBuilder result) {
        // Build a map of all exported packages in the system
        java.util.Map<String, List<PackageExport>> exportedPackages = new java.util.HashMap<>();

        for (Bundle b : ctx.getBundles()) {
            if (b.getState() == Bundle.UNINSTALLED) {
                continue;
            }

            String exportPackage = b.getHeaders().get(Constants.EXPORT_PACKAGE);
            if (exportPackage != null && !exportPackage.isEmpty()) {
                List<PackageInfo> exports = parsePackages(exportPackage);
                for (PackageInfo pkg : exports) {
                    if (pkg.name != null && !pkg.name.isEmpty() && pkg.name.contains(".")) {
                        exportedPackages
                                .computeIfAbsent(pkg.name, k -> new ArrayList<>())
                                .add(new PackageExport(b, pkg.name, pkg.version));
                    }
                }
            }
        }

        // Parse and check each imported package
        List<PackageInfo> imports = parsePackages(importPackageHeader);
        int missingCount = 0;
        int availableCount = 0;
        List<String> missingPackages = new ArrayList<>();

        for (PackageInfo importPkg : imports) {
            // Skip invalid package names
            if (importPkg.name == null || importPkg.name.isEmpty() || !importPkg.name.contains(".")) {
                continue;
            }

            List<PackageExport> availableExports = exportedPackages.get(importPkg.name);

            if (availableExports == null || availableExports.isEmpty()) {
                missingCount++;
                missingPackages.add("  ✗ " + importPkg.name
                        + (importPkg.version.isEmpty() ? "" : " " + importPkg.version)
                        + (importPkg.optional ? " (optional)" : "") + "\n");
            } else {
                availableCount++;
            }
        }

        // Only show missing packages
        if (missingCount > 0) {
            result.append("Missing Packages (")
                    .append(missingCount)
                    .append(" of ")
                    .append(imports.size())
                    .append(" imports):\n\n");
            for (String missing : missingPackages) {
                result.append(missing);
            }
            result.append(
                    "\n⚠ Action Required: Install bundles that provide the missing packages, or downgrade / change the dependencies.\n");
        } else {
            result.append("✓ All ").append(imports.size()).append(" imported packages are available.\n");
            result.append("Bundle should be resolvable. Check for other issues.\n");
        }
    }

    /**
     * Parse OSGi package header (Import-Package or Export-Package).
     * Handles complex cases with version ranges, attributes, and directives.
     */
    private List<PackageInfo> parsePackages(String header) {
        List<PackageInfo> packages = new ArrayList<>();
        if (header == null || header.isEmpty()) {
            return packages;
        }

        // State machine for parsing
        StringBuilder current = new StringBuilder();
        int depth = 0; // Track depth of quotes and brackets
        boolean inQuotes = false;

        for (int i = 0; i < header.length(); i++) {
            char c = header.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (!inQuotes && (c == '[' || c == '(')) {
                depth++;
                current.append(c);
            } else if (!inQuotes && (c == ']' || c == ')')) {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0 && !inQuotes) {
                // This is a package separator
                String pkg = current.toString().trim();
                if (!pkg.isEmpty()) {
                    packages.add(parsePackageEntry(pkg));
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Don't forget the last package
        String pkg = current.toString().trim();
        if (!pkg.isEmpty()) {
            packages.add(parsePackageEntry(pkg));
        }

        return packages;
    }

    /**
     * Parse a single package entry like "com.example.pkg;version="[1.0,2.0)";resolution:=optional"
     */
    private PackageInfo parsePackageEntry(String entry) {
        // Split on first semicolon to separate package name from attributes
        int semicolonPos = entry.indexOf(';');
        String packageName;
        String attributes;

        if (semicolonPos > 0) {
            packageName = entry.substring(0, semicolonPos).trim();
            attributes = entry.substring(semicolonPos + 1);
        } else {
            packageName = entry.trim();
            attributes = "";
        }

        // Extract version
        String version = "";
        if (attributes.contains("version=")) {
            int vStart = attributes.indexOf("version=") + 8;
            int vEnd = attributes.length();
            // Find the end of the version value (next semicolon outside quotes)
            boolean inQuote = false;
            for (int i = vStart; i < attributes.length(); i++) {
                char c = attributes.charAt(i);
                if (c == '"') {
                    inQuote = !inQuote;
                } else if (c == ';' && !inQuote) {
                    vEnd = i;
                    break;
                }
            }
            version = attributes.substring(vStart, vEnd).trim().replaceAll("\"", "");
        }

        boolean optional = attributes.contains("resolution:=optional");

        return new PackageInfo(packageName, version, optional);
    }

    private String extractVersion(String exportEntry) {
        if (exportEntry.contains("version=")) {
            int start = exportEntry.indexOf("version=") + 8;
            int end = exportEntry.indexOf(";", start);
            if (end == -1) {
                end = exportEntry.length();
            }
            return exportEntry.substring(start, end).replaceAll("\"", "").trim();
        }
        return "0.0.0";
    }

    private static class PackageInfo {
        final String name;
        final String version;
        final boolean optional;

        PackageInfo(String name, String version, boolean optional) {
            this.name = name;
            this.version = version;
            this.optional = optional;
        }
    }

    private static class PackageExport {
        final Bundle bundle;
        final String packageName;
        final String version;

        PackageExport(Bundle bundle, String packageName, String version) {
            this.bundle = bundle;
            this.packageName = packageName;
            this.version = version;
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
