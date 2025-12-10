# OSGi Bundle Diagnostics for AEM/Sling

This document explains the OSGi diagnostic capabilities added to your MCP server to help developers troubleshoot bundle and component issues directly from Cursor.

## Overview

The diagnostic system consists of three main components:

1. **OsgiBundleDiagnosticContribution** - MCP Tool for analyzing bundle/component issues
2. **OsgiDiagnosticPromptContribution** - MCP Prompt that guides developers through troubleshooting
3. **Enhanced Resources** - Bundle and Component resources for inspection

## How It Works

### 1. The Diagnostic Tool: `diagnose-osgi-bundle`

This MCP tool provides detailed diagnostic information about OSGi bundles and components.

**Usage from Cursor:**
- Ask Cursor: "Why isn't my bundle starting?"
- Ask Cursor: "Diagnose bundle com.example.myapp"
- Ask Cursor: "Check all problematic bundles"

**What it analyzes:**
- Bundle state (INSTALLED, RESOLVED, ACTIVE, etc.)
- Unsatisfied package imports
- Missing dependencies
- Component service references
- Configuration issues
- Fragment bundle status

**Example output:**
```
=== Bundle Diagnostic Report ===

Bundle: com.example.myapp
Version: 1.0.0
State: INSTALLED

✗ Bundle is NOT active. Analyzing issues...

⚠ Bundle is INSTALLED but not RESOLVED
This typically means there are unsatisfied dependencies.

Unsatisfied Requirements:
  ✗ osgi.wiring.package: {filter=(package=org.apache.sling.api)}
    Missing package: (package=org.apache.sling.api)
  ✗ osgi.wiring.package: {filter=(package=com.google.gson)}
    Missing package: (package=com.google.gson)

--- Declarative Services Components ---

Component: com.example.myapp.services.MyService
  State: UNSATISFIED_REFERENCE
  ✗ Unsatisfied Service References:
    - dataSource (javax.sql.DataSource)
```

### 2. The Diagnostic Prompt: `diagnose-osgi-issue`

This prompt teaches Cursor how to interpret OSGi issues and provide solutions.

**Usage from Cursor:**
- "Use the diagnose-osgi-issue prompt"
- "Help me fix my bundle issues"

**What it provides:**
- Step-by-step troubleshooting guide
- Common issue patterns and solutions
- Explanations of bundle states
- Component state interpretations
- Actionable fix recommendations

### 3. Resources for Inspection

**Bundle Resource** (`bundle://`)
- Lists all bundles and their states
- Quick overview of system health

**Component Resource** (`component://`)
- Lists all OSGi components and their states
- Identifies unsatisfied components

**Template Resources:**
- `bundles://state/resolved` - Filter bundles by state
- `components://state/active` - Filter components by state

## Common Scenarios

### Scenario 1: Bundle Won't Start After Deployment

**Developer asks:** "My bundle com.example.myapp won't start"

**Cursor will:**
1. Call the `diagnose-osgi-bundle` tool with the bundle name
2. Analyze the output
3. Identify missing dependencies (e.g., missing package imports)
4. Suggest adding dependencies to `pom.xml`
5. Provide the exact Maven dependency to add

### Scenario 2: Component Not Activating

**Developer asks:** "Why isn't my component activating?"

**Cursor will:**
1. Use the diagnostic tool to check component state
2. Identify unsatisfied service references
3. Check if the required service provider bundle is active
4. Suggest making the reference optional or deploying the missing bundle
5. Show how to update the `@Reference` annotation

### Scenario 3: System-Wide Issues

**Developer asks:** "Are there any bundle issues in my AEM instance?"

**Cursor will:**
1. Run diagnostic tool without parameters (scans all bundles)
2. List all problematic bundles
3. Prioritize issues by severity
4. Provide a summary of what needs attention

## Technical Details

### Dependencies Added

The following dependencies were added to `pom.xml`:

```xml
<dependency>
    <groupId>org.osgi</groupId>
    <artifactId>org.osgi.service.component</artifactId>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.osgi</groupId>
    <artifactId>org.osgi.resource</artifactId>
    <scope>provided</scope>
</dependency>
```

### Files Created

1. `OsgiBundleDiagnosticContribution.java` - The diagnostic tool implementation
2. `OsgiDiagnosticPromptContribution.java` - The troubleshooting guide prompt
3. `ComponentResourceContribution.java` - Component inspection resource

### How to Deploy

1. Build the bundle:
   ```bash
   mvn clean install
   ```

2. Deploy to your AEM/Sling instance:
   ```bash
   mvn sling:install
   ```
   Or manually upload the JAR to the Felix Console

3. Verify the MCP server is running:
   - Check `/bin/mcp` endpoint
   - Verify the bundle is ACTIVE in Felix Console

4. Connect Cursor to your MCP server (if not already connected)

## Using from Cursor

Once deployed, you can interact with the diagnostic system naturally:

**Example conversations:**

```
You: "My bundle com.mycompany.core isn't starting"
Cursor: [Calls diagnose-osgi-bundle tool]
        "Your bundle is in INSTALLED state because it's missing the 
        org.apache.sling.models.api package. Add this dependency to your pom.xml:
        <dependency>
            <groupId>org.apache.sling</groupId>
            <artifactId>org.apache.sling.models.api</artifactId>
            <version>1.5.0</version>
            <scope>provided</scope>
        </dependency>"
```

```
You: "Check if there are any bundle problems"
Cursor: [Calls diagnose-osgi-bundle without parameters]
        "Found 2 problematic bundles:
        1. com.example.integration - Missing service reference to EmailService
        2. com.example.workflow - Bundle is RESOLVED but not started
        
        Would you like me to investigate either of these?"
```

## Benefits

1. **Faster Debugging** - No need to manually check Felix Console
2. **Contextual Help** - Cursor understands OSGi concepts and provides relevant solutions
3. **Proactive Detection** - Can scan for issues before they cause problems
4. **Learning Tool** - Developers learn OSGi patterns through guided troubleshooting
5. **Integration** - Works seamlessly with your existing development workflow in Cursor

## Future Enhancements

Potential additions:
- Automatic dependency resolution suggestions
- Integration with Maven Central search
- Bundle wiring visualization
- Historical issue tracking
- Performance diagnostics
- Memory leak detection for bundles

## Troubleshooting the Diagnostic System

If the diagnostic tools aren't working:

1. Verify the MCP server bundle is ACTIVE
2. Check that ServiceComponentRuntime service is available
3. Ensure proper permissions for bundle inspection
4. Check MCP server logs for errors

## Support

For issues or questions about this diagnostic system, check:
- Apache Sling documentation: https://sling.apache.org
- OSGi specification: https://docs.osgi.org
- MCP protocol: https://modelcontextprotocol.io

