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

import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import org.apache.sling.mcp.server.spi.McpServerContribution;
import org.osgi.service.component.annotations.Component;

/**
 * MCP Prompt that helps developers diagnose and fix OSGi bundle issues.
 * This prompt teaches Cursor how to:
 * - Identify why bundles aren't starting
 * - Understand common OSGi issues
 * - Provide actionable solutions
 */
@Component
public class OsgiDiagnosticPromptContribution implements McpServerContribution {

    @Override
    public List<SyncPromptSpecification> getSyncPromptSpecification() {
        return List.of(new SyncPromptSpecification(
                new Prompt(
                        "diagnose-osgi-issue",
                        "Diagnose OSGi Bundle Issues",
                        "Helps diagnose why an OSGi bundle or component isn't starting. Provides step-by-step troubleshooting guidance.",
                        List.of(new PromptArgument(
                                "bundle-name",
                                "Bundle Symbolic Name",
                                "The symbolic name of the bundle that isn't starting (optional - if not provided, will check all bundles)",
                                false))),
                (context, request) -> {
                    String bundleName = (String) request.arguments().get("bundle-name");

                    String instructions = buildDiagnosticInstructions(bundleName);

                    PromptMessage msg = new PromptMessage(Role.ASSISTANT, new McpSchema.TextContent(instructions));

                    return new GetPromptResult("OSGi Diagnostic Guide", List.of(msg));
                }));
    }

    private String buildDiagnosticInstructions(String bundleName) {
        StringBuilder sb = new StringBuilder();

        sb.append("# OSGi Bundle Diagnostic Assistant\n\n");

        if (bundleName != null && !bundleName.isEmpty()) {
            sb.append("I'll help you diagnose why the bundle '")
                    .append(bundleName)
                    .append("' isn't starting.\n\n");
            sb.append("## Step 1: Run Diagnostic Tool\n\n");
            sb.append("First, use the `diagnose-osgi-bundle` tool with bundleSymbolicName='")
                    .append(bundleName)
                    .append("'\n\n");
        } else {
            sb.append("I'll help you diagnose OSGi bundle issues in your environment.\n\n");
            sb.append("## Step 1: Identify Problematic Bundles\n\n");
            sb.append("Use the `diagnose-osgi-bundle` tool without parameters to scan all bundles.\n\n");
        }

        sb.append("## Step 2: Interpret Common Issues\n\n");

        sb.append("### Bundle State: INSTALLED (Not Resolved)\n");
        sb.append("**Problem**: Bundle dependencies aren't satisfied.\n\n");
        sb.append("**Common Causes**:\n");
        sb.append(
                "- Missing package imports: Another bundle that exports the required package isn't installed or active\n");
        sb.append("- Version conflicts: Required package version doesn't match available versions\n");
        sb.append("- Missing bundle: A required bundle hasn't been deployed\n\n");
        sb.append("**Solutions**:\n");
        sb.append("1. Check the diagnostic output for 'Unsatisfied Requirements'\n");
        sb.append("2. Look for the missing packages in the Import-Package errors\n");
        sb.append("3. Use `bundle://` resource to find bundles that export the needed packages\n");
        sb.append("4. Install missing bundles or update bundle manifests to match available versions\n\n");

        sb.append("### Bundle State: RESOLVED (Not Active)\n");
        sb.append("**Problem**: Bundle has all dependencies but hasn't been started.\n\n");
        sb.append("**Common Causes**:\n");
        sb.append("- Bundle has lazy activation policy\n");
        sb.append("- Bundle is a fragment (fragments never become ACTIVE)\n");
        sb.append("- Manual start required\n\n");
        sb.append("**Solutions**:\n");
        sb.append("1. If it's not a fragment, the bundle might just need to be started\n");
        sb.append("2. Check if the bundle has Bundle-ActivationPolicy: lazy\n");
        sb.append("3. Fragments are normal - they attach to their host bundle\n\n");

        sb.append("### Component State: UNSATISFIED_REFERENCE\n");
        sb.append("**Problem**: Component can't find required OSGi services.\n\n");
        sb.append("**Common Causes**:\n");
        sb.append("- Required service isn't registered (bundle providing it isn't active)\n");
        sb.append("- Service filter doesn't match any available services\n");
        sb.append("- Circular dependency between components\n\n");
        sb.append("**Solutions**:\n");
        sb.append("1. Check the 'Unsatisfied Service References' in the diagnostic output\n");
        sb.append("2. Use `component://` resource to verify the service provider is active\n");
        sb.append("3. Check if the target filter is too restrictive\n");
        sb.append("4. Make the reference optional if possible (cardinality=OPTIONAL)\n\n");

        sb.append("### Component State: UNSATISFIED_CONFIGURATION\n");
        sb.append("**Problem**: Component requires configuration that hasn't been provided.\n\n");
        sb.append("**Common Causes**:\n");
        sb.append("- Missing OSGi configuration in /apps or /libs\n");
        sb.append("- Configuration not deployed to the environment\n");
        sb.append("- Wrong configuration PID\n\n");
        sb.append("**Solutions**:\n");
        sb.append("1. Check if component has @Designate annotation requiring config\n");
        sb.append("2. Verify configuration exists in repository or as .config file\n");
        sb.append("3. Check configuration PID matches component name\n");
        sb.append("4. Make configuration optional by removing 'required' policy\n\n");

        sb.append("## Step 3: Apply Fixes\n\n");
        sb.append("Based on the diagnostic results, I'll help you:\n");
        sb.append("1. Identify which dependencies need to be added to your pom.xml\n");
        sb.append("2. Fix package import/export statements in bnd.bnd or MANIFEST.MF\n");
        sb.append("3. Create missing OSGi configurations\n");
        sb.append("4. Update component annotations to fix reference issues\n");
        sb.append("5. Suggest architectural changes if circular dependencies are detected\n\n");

        sb.append("## Step 4: Verify Fix\n\n");
        sb.append("After applying fixes:\n");
        sb.append("1. Rebuild and redeploy the bundle\n");
        sb.append("2. Run the diagnostic tool again to verify all issues are resolved\n");
        sb.append("3. Check that the bundle state is ACTIVE\n");
        sb.append("4. Verify components are in ACTIVE or SATISFIED state\n\n");

        sb.append("Let me know what the diagnostic tool returns, and I'll provide specific solutions for your issue.");

        return sb.toString();
    }
}
