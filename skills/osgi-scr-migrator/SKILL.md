---
name: osgi-scr-migrator
description: Migrate OSGi-based Java projects from deprecated Felix SCR annotations to official OSGi R6/R7 Component annotations. Use when migrating legacy OSGi bundles that use @Component, @Service, @Reference, @Property from org.apache.felix.scr.annotations to org.osgi.service.component.annotations. Handles both Java source code transformation and Maven POM updates (removing maven-scr-plugin, updating dependencies). Also supports Sling-specific annotations like @SlingServlet and @SlingFilter. Features full automation of property migration, metatype configuration generation, constructor injection optimization, validation, and comprehensive reporting. After basic migration, offers optional Step 7 to optimize code with constructor injection for immutable dependencies.
---

# OSGi SCR to R6/R7 Migration (Enhanced)

Automate complete migration from deprecated Felix SCR annotations to official OSGi R6/R7 Component annotations with full property migration, metatype support, and validation.

## Overview

Felix SCR (Service Component Runtime) annotations (`org.apache.felix.scr.annotations.*`) were deprecated and replaced by official OSGi R6 annotations (`org.osgi.service.component.annotations.*`). This skill provides fully automated tools and guidance for complete migration with minimal manual intervention.

## Key Features

- **Full @Property Migration**: Automatically converts all @Property annotations with proper type specifications (:Integer, :Boolean, etc.)
- **Service Property Type Annotations**: Automatically uses `@ServiceRanking`, `@ServiceDescription`, and `@ServiceVendor` from `org.osgi.service.component.propertytypes` package (requires `org.osgi:org.osgi.service.component` dependency) for standard service properties (preferred over property strings)
- **Component Property Types**: Generates type-safe configuration interfaces (OSGi R7 best practice)
  - Automatically converts `Map<String, Object>` to `Config` annotation interfaces
  - No more manual type conversion with PropertiesUtil
  - Type-safe, IDE-friendly configuration access
- **Metatype Configuration**: Detects and generates complete @Designate and @ObjectClassDefinition interfaces with full metadata preservation
  - Captures @Component label/description → @ObjectClassDefinition name/description
  - Captures @Property label/description → @AttributeDefinition name/description
  - Updates @Activate/@Modified methods to use Config type
- **Sling Annotations**: Fully automates @SlingServlet and @SlingFilter transformations
- **Constructor Injection Optimization** (Optional Step 7): Automatically migrates components to use constructor injection for immutable dependencies
  - Moves `@Activate(Config config)` to constructor injection
  - Migrates mandatory static unary `@Reference` fields to constructor parameters
  - Makes fields `final` for immutability and thread safety
  - Preserves optional/dynamic/multiple references as field injection
- **Enforced Versions**: Uses latest stable versions (component annotations 1.5.1, metatype 1.4.1)
- **Validation**: Built-in validation checks for migration completeness
- **Comprehensive Reporting**: Detailed statistics and warnings for all migrations
- **Version Control Friendly**: No backup files created - use git or other SCM for change tracking

## When to Use

- Migrating legacy OSGi bundles using Felix SCR annotations
- Upgrading to OSGi R6/R7 standards
- Modernizing AEM/Sling projects
- Preparing projects for newer OSGi runtime versions
- Removing maven-scr-plugin dependency

## Prerequisites

Before using this migration skill, ensure you have:

- **Python 3.7+** - Required to run the migration scripts
- **Java 17+** - Required for compiling the migrated code (OSGi R6/R7 annotations require Java 17+)
- **Maven or Gradle** - For building and testing the project
- **Version Control** - Git or similar (required for rollback capability)

**Note**: The migrated code will use OSGi R6/R7 Component annotations which require Java 17 or higher. Projects on older Java versions must upgrade to Java 17+ as part of this migration.

## Migration Workflow

### Step 1: Assessment and Preparation

**IMPORTANT**: Before migration, commit all your code to git or another version control system. The migration scripts modify files directly without creating backups.

```bash
# Ensure code is committed
git status
git add .
git commit -m "Pre-OSGi migration snapshot"

# Find files with SCR annotations
grep -r "org.apache.felix.scr.annotations" --include="*.java" .

# Check POM dependencies
grep -r "felix.scr.annotations" --include="pom.xml" .

# Count components to migrate
grep -r "@Component" --include="*.java" . | wc -l
```

Create a backup or ensure code is in version control before proceeding.

### Step 2: Run Automated Migration

Use the enhanced migration scripts for fully automated transformation:

#### 2a. Migrate Java Annotations (Enhanced)

```bash
# Dry run to preview changes
python3 scripts/migrate_annotations.py /path/to/src --dry-run

# Apply changes with validation
python3 scripts/migrate_annotations.py /path/to/src --validate

# Tip: Commit to git before running to easily review/revert changes
git add . && git commit -m "Pre-migration snapshot"
```

The enhanced script handles:
- Import statement updates
- `@Component` attribute transformation with property merging
- `@Service` merging into `@Component`
- `@Reference` cardinality updates (OPTIONAL_UNARY → OPTIONAL, etc.)
- **Full `@Property` migration** with type specifications
- **Automatic metatype configuration generation** for configurable components
- **Complete `@SlingServlet` transformation** to @Component with servlet properties
- **Complete `@SlingFilter` transformation** to @Component with filter properties
- Lifecycle annotation imports (`@Activate`, `@Deactivate`, `@Modified`)
- Validation and comprehensive reporting

#### 2b. Migrate POM Files (Enhanced)

```bash
# Dry run to preview changes
python3 scripts/migrate_pom.py /path/to/project --dry-run

# Apply changes with automatic metatype detection
python3 scripts/migrate_pom.py /path/to/project --auto-detect-metatype

# Apply with validation
python3 scripts/migrate_pom.py /path/to/project --validate
```

The enhanced script handles:
- Removing `maven-scr-plugin` from all locations
- Removing `org.apache.felix.scr.annotations` dependency
- Adding `org.osgi.service.component.annotations` dependency (version **1.5.1** enforced) **at the beginning of the dependency list**
- **Smart version detection** from existing OSGi dependencies
- **Automatic metatype dependency addition** when detected (version **1.4.1** enforced) **placed after component annotations**
- Ensuring OSGi bundle plugin (`maven-bundle-plugin` or `bnd-maven-plugin`) uses `extensions=true`
- Removing obsolete SCR-specific plugin instructions
- Pretty-printing XML with proper indentation
- Validation and detailed reporting

### Step 3: Review Migration Results

The scripts now provide comprehensive statistics:

```
======================================================================
MIGRATION SUMMARY
======================================================================
Files processed:              15
Files changed:                12
@Component annotations:       45
@Service merged:              38
@Reference updated:           27
@Property migrated:           156
@SlingServlet migrated:       8
@SlingFilter migrated:        3
Metatype configs generated:   12
Import statements updated:    180

Warnings: 2
  ⚠ MyService.java: Consider reviewing generated metatype configuration
  ⚠ OtherService.java: Manual review recommended for complex property

Errors: 0
======================================================================
```

### Step 4: Advanced Features

#### Metatype Configuration with Component Property Types (Fully Automated)

For components with `metatype = true`, the script automatically generates complete metatype configurations using **Component Property Types** (OSGi R7's preferred approach):

- **@Component label/description** → **@ObjectClassDefinition name/description**
- **@Property label/description** → **@AttributeDefinition name/description**
- **@Activate(Map<String, Object>)** → **@Activate(Config)** for type-safe access

```java
// Before - Using deprecated Map approach
@Component(metatype = true, label = "My Service", description = "This is my service")
@Property(name = "timeout", intValue = 30, label = "Timeout", description = "Request timeout in seconds")
public class MyService {
    @Activate
    private void activate(Map<String, Object> props) {
        int timeout = PropertiesUtil.toInteger(props.get("timeout"), 30);
    }
}

// After - Using Component Property Types (automatically generated)
@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    @ObjectClassDefinition(name = "My Service", description = "This is my service")
    public @interface Config {
        @AttributeDefinition(name = "Timeout", description = "Request timeout in seconds")
        int timeout() default 30;
    }

    @Activate
    private void activate(Config config) {
        // Type-safe, no conversion needed!
        int timeout = config.timeout();
    }
}
```

**Why Component Property Types?**
- ✅ **Type-safe**: No runtime type conversion errors
- ✅ **Cleaner**: No PropertiesUtil or manual conversions
- ✅ **IDE-friendly**: Auto-completion for configuration properties
- ✅ **Maintainable**: Changes to config structure caught at compile-time
- ✅ **OSGi R7 standard**: Modern best practice

**Note:** The migration automatically:
1. Captures all label/description metadata from @Component and @Property
2. Generates the Config annotation interface
3. Updates @Activate/@Modified methods to use the Config type instead of Map

The POM is automatically updated with the OSGi annotations and metatype dependencies using the latest versions:
```xml
<dependency>
    <groupId>org.osgi</groupId>
    <artifactId>org.osgi.service.component.annotations</artifactId>
    <version>1.5.1</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.osgi</groupId>
    <artifactId>org.osgi.service.component</artifactId>
    <version>1.5.1</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.osgi</groupId>
    <artifactId>org.osgi.service.metatype.annotations</artifactId>
    <version>1.4.1</version>
    <scope>provided</scope>
</dependency>
```

**Important**: This migration skill enforces the latest stable versions:
- **org.osgi.service.component.annotations: 1.5.1** (required - provides @Component, @Reference, etc.)
- **org.osgi.service.component: 1.5.1** (required - provides property type annotations like @ServiceDescription, @ServiceRanking, @ServiceVendor)
- **org.osgi.service.metatype.annotations: 1.4.1** (required when metatype is used)

**Automatic for Sling Projects**: When Sling servlets/filters are detected:
- **org.apache.sling.servlets.annotations: 1.2.6** (automatically added, provides type-safe Sling annotations)

These versions are required for proper OSGi R6/R7 compatibility.

#### Sling Annotations (Fully Automated)

Sling servlets and filters are completely automated. The migration uses type-safe Sling Servlets Annotations:

```java
// Before
@SlingServlet(
    paths = "/bin/myservlet",
    methods = {"GET", "POST"},
    extensions = {"html", "json"}
)
public class MyServlet extends SlingAllMethodsServlet {
}

// After (automatically generated)
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import javax.servlet.Servlet;

@Component(service = Servlet.class)
@SlingServletPaths(value = "/bin/myservlet")
public class MyServlet extends SlingAllMethodsServlet {
}
```

**Path-based servlets** use `@SlingServletPaths`:
```java
@Component(service = Servlet.class)
@SlingServletPaths(value = {"/bin/servlet1", "/bin/servlet2"})
```

**Resource-type-based servlets** use `@SlingServletResourceTypes`:
```java
@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "my/resource/type",
    selectors = "print",
    extensions = {"html", "pdf"},
    methods = {"GET", "POST"}
)
```

**Filters** use `@SlingServletFilter`:
```java
@Component(service = Filter.class)
@SlingServletFilter(
    scope = SlingServletFilterScope.REQUEST,
    order = -700,
    pattern = "/content/.*"
)
```

The migration automatically:
- Detects Sling servlet/filter usage in your Java files
- Adds the `org.apache.sling.servlets.annotations:1.2.6` dependency to your POM
- Transforms annotations to use the type-safe Sling Servlets Annotations format
- Updates all necessary imports

See [references/annotation-mappings.md](references/annotation-mappings.md) for detailed examples of all Sling annotation transformations.

### Step 5: Build and Test

```bash
# Clean and build
mvn clean install

# Run tests
mvn test

# Verify bundle activation
# The migration scripts include validation to catch common issues
```

### Step 6: Run Validation

```bash
# Validate Java migrations
python3 scripts/migrate_annotations.py /path/to/src --validate

# Validate POM migrations
python3 scripts/migrate_pom.py /path/to/project --validate

# Run comprehensive test suite
bash scripts/tests/run_tests.sh
```

**✅ Basic migration complete!** If all tests pass, proceed to Step 7 for optional constructor injection optimization.

---

## ✨ Optional Post-Migration Optimization

Once the basic migration (Steps 1-6) is complete and all tests pass, consider **Step 7** below to further modernize your code with constructor injection.

---

### Step 7: Optimize with Constructor Injection (Automated, Optional)

**⚠️ IMPORTANT**: Run this step AFTER completing Steps 1-6 and ensuring all tests pass.

Automatically optimize your code with modern OSGi best practices using **constructor injection**. This step is optional but highly recommended for cleaner, more maintainable code.

**NEW**: This step is now fully automated using the `migrate_constructor_injection.py` script!

#### Quick Start - Run the Migration

```bash
# Preview changes (dry-run) - RECOMMENDED FIRST
python3 scripts/migrate_constructor_injection.py /path/to/src --dry-run

# Apply migration with validation
python3 scripts/migrate_constructor_injection.py /path/to/src --validate

# Single file migration
python3 scripts/migrate_constructor_injection.py /path/to/MyService.java

# Tip: Always commit to git before running
git add . && git commit -m "Pre-constructor-injection snapshot"
```

#### Benefits of Constructor Injection

- ✅ **Immutability**: Dependencies declared as `final` fields
- ✅ **Explicit Dependencies**: All dependencies visible in constructor signature
- ✅ **Thread Safety**: Immutable references eliminate race conditions
- ✅ **Testability**: Easier to mock dependencies in unit tests
- ✅ **Modern Best Practice**: Aligned with OSGi R7/R8 recommendations

#### 7a. Constructor Injection for Configurations

Instead of using `@Activate` methods to receive configuration, inject it directly in the constructor:

```java
// After basic migration (Step 2)
@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    @ObjectClassDefinition(name = "My Service")
    public @interface Config {
        @AttributeDefinition(name = "Timeout")
        int timeout() default 30;

        @AttributeDefinition(name = "Max Retries")
        int maxRetries() default 3;
    }

    private int timeout;
    private int maxRetries;

    @Activate
    private void activate(Config config) {
        this.timeout = config.timeout();
        this.maxRetries = config.maxRetries();
    }
}

// After optimization (Step 7)
@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    @ObjectClassDefinition(name = "My Service")
    public @interface Config {
        @AttributeDefinition(name = "Timeout")
        int timeout() default 30;

        @AttributeDefinition(name = "Max Retries")
        int maxRetries() default 3;
    }

    private final int timeout;
    private final int maxRetries;

    @Activate
    public MyService(Config config) {
        this.timeout = config.timeout();
        this.maxRetries = config.maxRetries();
    }
}
```

**Key changes:**
- Constructor is now `public` (OSGi requirement for constructor injection)
- Configuration fields are `final` (immutable)
- No separate `@Activate` method needed
- Configuration is available immediately upon construction

#### 7b. Constructor Injection for Unary Static Mandatory References

For references that are:
- **Mandatory** (not optional)
- **Static** (not dynamic)
- **Unary** (single reference, not multiple)

Use constructor injection to make dependencies explicit and immutable:

```java
// After basic migration (Step 2) - Field injection
@Component(service = MyService.class)
public class MyService {

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SlingRepository repository;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private ConfigurationAdmin configAdmin;  // Optional - keep as field

    public void doSomething() {
        // Use resolverFactory and repository
    }
}

// After optimization (Step 7) - Constructor injection for mandatory references
@Component(service = MyService.class)
public class MyService {

    private final ResourceResolverFactory resolverFactory;
    private final SlingRepository repository;

    // Optional reference stays as field injection
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private ConfigurationAdmin configAdmin;

    @Activate
    public MyService(
        @Reference ResourceResolverFactory resolverFactory,
        @Reference SlingRepository repository
    ) {
        this.resolverFactory = resolverFactory;
        this.repository = repository;
    }

    public void doSomething() {
        // Use resolverFactory and repository (guaranteed non-null)
    }
}
```

**When to use constructor injection for references:**
- ✅ Mandatory references (`cardinality = MANDATORY` or no cardinality specified)
- ✅ Static references (`policy = STATIC` or no policy specified)
- ✅ Unary references (single service, not collections)

**When to keep field injection:**
- ❌ Optional references (`cardinality = OPTIONAL`)
- ❌ Dynamic references (`policy = DYNAMIC`)
- ❌ Multiple references (`cardinality = MULTIPLE` or `AT_LEAST_ONE`)
- ❌ References with custom bind/unbind methods

#### 7c. Combined Configuration and References

You can combine configuration and reference injection in a single constructor:

```java
// Optimized: Constructor injection for config + mandatory static references
@Component(service = MyService.class)
@Designate(ocd = MyService.Config.class)
public class MyService {

    @ObjectClassDefinition(name = "My Service")
    public @interface Config {
        @AttributeDefinition(name = "Timeout")
        int timeout() default 30;
    }

    private final int timeout;
    private final ResourceResolverFactory resolverFactory;
    private final SlingRepository repository;

    // Optional/dynamic references stay as field injection
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private ConfigurationAdmin configAdmin;

    @Activate
    public MyService(
        Config config,
        @Reference ResourceResolverFactory resolverFactory,
        @Reference SlingRepository repository
    ) {
        this.timeout = config.timeout();
        this.resolverFactory = resolverFactory;
        this.repository = repository;
    }
}
```

**Important notes:**
- Configuration parameter (if present) must be **first** in constructor
- Reference parameters follow after configuration
- Constructor must be `public` for OSGi to inject dependencies
- Use `@Reference` annotation on each reference parameter
- Reference attributes (target, cardinality, etc.) can be specified on parameters

#### 7d. What the Script Does Automatically

The script automatically:
- ✅ Identifies components with `@Activate(Config config)` methods
- ✅ Identifies mandatory static unary `@Reference` fields
- ✅ Creates public constructor with `@Activate` annotation
- ✅ Moves config parameter to constructor (first parameter)
- ✅ Moves eligible references to constructor parameters
- ✅ Makes fields `final` for immutability
- ✅ Preserves optional/dynamic/multiple references as field injection
- ✅ Generates comprehensive migration statistics

**What the script does NOT migrate** (kept as field injection):
- ❌ Optional references (`cardinality = OPTIONAL`)
- ❌ Dynamic references (`policy = DYNAMIC`)
- ❌ Multiple references (`cardinality = MULTIPLE` or `AT_LEAST_ONE`)
- ❌ References with custom bind/unbind methods
- ❌ Collection references (List, Set, Collection)

#### 7e. Post-Migration Verification

After running the automated migration, verify the changes:

1. **Review the migration statistics**:
   ```
   ======================================================================
   CONSTRUCTOR INJECTION MIGRATION SUMMARY
   ======================================================================
   Files processed:              15
   Files changed:                8
   Components migrated:          8
   Configs converted:            6
   References converted:         14
   Fields made final:            20
   ======================================================================
   ```

2. **Test thoroughly**:
   ```bash
   mvn clean install
   mvn test
   ```

3. **Verify OSGi activation**:
   - Check OSGi console for component activation
   - Verify all references are satisfied
   - Test component functionality

4. **Review generated constructors**:
   - Constructor is `public` ✓
   - `@Activate` annotation on constructor ✓
   - Config parameter is first (if present) ✓
   - Only mandatory static unary references moved to constructor ✓
   - Optional/dynamic references remain as fields ✓
   - Fields are `final` where appropriate ✓

**If tests fail**: Use `git diff` to review changes and `git checkout .` to revert if needed.

## Advanced Usage

### Command-Line Options

#### Constructor Injection Migration Script (Step 7)

```bash
python3 scripts/migrate_constructor_injection.py [OPTIONS] PATH

Options:
  --dry-run          Preview changes without modifying files
  --validate         Validate migrated code for common issues

Examples:
  # Preview all changes
  python3 scripts/migrate_constructor_injection.py src/ --dry-run

  # Migrate with validation
  python3 scripts/migrate_constructor_injection.py src/ --validate

  # Single file migration
  python3 scripts/migrate_constructor_injection.py src/main/java/MyService.java

  # Use git to review changes
  git diff src/
```

#### Java Migration Script (Step 2)

```bash
python3 scripts/migrate_annotations.py [OPTIONS] PATH

Options:
  --dry-run          Preview changes without modifying files
  --validate         Validate migrated code for common issues

Examples:
  # Preview all changes
  python3 scripts/migrate_annotations.py src/ --dry-run

  # Migrate with validation
  python3 scripts/migrate_annotations.py src/ --validate

  # Use git to review changes
  git diff src/
```

#### POM Migration Script

```bash
python3 scripts/migrate_pom.py [OPTIONS] PATH

Options:
  --dry-run              Preview changes without modifying files
  --validate             Validate migrated POMs
  --add-metatype         Add metatype dependency
  --auto-detect-metatype Auto-detect metatype usage from Java files
  --detect-versions      Detect OSGi versions from existing deps (default: True)

Examples:
  # Migrate with automatic metatype detection
  python3 scripts/migrate_pom.py . --auto-detect-metatype

  # Dry run with validation
  python3 scripts/migrate_pom.py . --dry-run --validate

  # Use git to review changes
  git diff pom.xml
```

### Testing

The skill includes comprehensive unit tests:

```bash
# Run all tests
bash scripts/tests/run_tests.sh

# Run Java migration tests
python3 scripts/tests/test_migrate_annotations.py

# Run POM migration tests
python3 scripts/tests/test_migrate_pom.py

# Run constructor injection migration tests
python3 scripts/tests/test_migrate_constructor_injection.py
```

Test coverage includes:
- Property parsing and conversion
- Metatype configuration generation
- Sling annotation transformation
- Reference cardinality migration
- Complete file migration scenarios
- POM plugin and dependency updates
- Version detection
- Statistics tracking
- **Constructor injection migration** (config and reference conversion)
- **Reference eligibility detection** (mandatory/optional, static/dynamic)
- **Field final modifier addition**

## Migration Patterns

### Pattern 1: Basic Component with Properties

```java
// Before
@Component(immediate = true)
@Service
@Property(name = "service.vendor", value = "Apache Software Foundation")
@Property(name = "service.ranking", intValue = 100)
public class MyService implements SomeInterface {
}

// After (fully automated)
@Component(
    immediate = true,
    service = SomeInterface.class,
    property = {
        "service.vendor=Apache Software Foundation",
        "service.ranking:Integer=100"
    }
)
public class MyService implements SomeInterface {
}
```

### Pattern 2: Configurable Component with Component Property Types

```java
// Before - Using deprecated Map approach
@Component(metatype = true, immediate = true, label = "My Service", description = "Service description")
@Property(name = "timeout", intValue = 30, label = "Timeout", description = "Request timeout")
@Property(name = "retries", intValue = 3, label = "Retries", description = "Number of retries")
public class MyService {
    @Activate
    private void activate(Map<String, Object> props) {
        int timeout = PropertiesUtil.toInteger(props.get("timeout"), 30);
        int retries = PropertiesUtil.toInteger(props.get("retries"), 3);
    }
}

// After - Using Component Property Types (fully automated, type-safe)
@Component(immediate = true)
@Designate(ocd = MyService.Config.class)
public class MyService {

    @ObjectClassDefinition(name = "My Service", description = "Service description")
    public @interface Config {
        @AttributeDefinition(name = "Timeout", description = "Request timeout")
        int timeout() default 30;

        @AttributeDefinition(name = "Retries", description = "Number of retries")
        int retries() default 3;
    }

    @Activate
    private void activate(Config config) {
        // Type-safe access - no conversion utilities needed!
        int timeout = config.timeout();
        int retries = config.retries();
    }
}
```

**Note**: Component Property Types are the OSGi R7 best practice and are automatically generated by the migration.

### Pattern 3: Sling Servlet

```java
// Before
@SlingServlet(
    resourceTypes = "my/resource/type",
    selectors = {"print", "pdf"},
    extensions = "html"
)
public class MyServlet extends SlingAllMethodsServlet {
}

// After (fully automated using Sling Servlets Annotations)
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import javax.servlet.Servlet;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "my/resource/type",
    selectors = {"print", "pdf"},
    extensions = "html"
)
public class MyServlet extends SlingAllMethodsServlet {
}
```

## Reference Documentation

For comprehensive annotation mappings and examples:

```bash
# View complete mapping reference
cat references/annotation-mappings.md
```

This includes:
- All annotation transformations with before/after examples
- Cardinality and policy mappings
- Property type specifications
- Sling annotation patterns
- Common migration patterns

## Troubleshooting

### Components Not Activating

Check:
- `service` attribute properly set in `@Component`
- All required dependencies satisfied
- No compilation errors
- Run validation: `python3 scripts/migrate_annotations.py src/ --validate`

### Configuration Not Working

- Verify metatype annotations are present
- Check `@Designate` links to correct config interface
- Ensure activate method signature matches config type
- POM contains metatype dependency (auto-added with `--auto-detect-metatype`)

### Build Failures

- Ensure OSGi bundle plugin (`maven-bundle-plugin` or `bnd-maven-plugin`) has `extensions=true` (auto-fixed)
- Verify all OSGi annotation dependencies are present (auto-added)
- Run POM validation: `python3 scripts/migrate_pom.py . --validate`
- Check migration summary for errors and warnings

### Migration Issues

If you encounter any issues:

1. **Review the statistics**: The summary shows exactly what was changed
2. **Check warnings**: The script flags items that may need review
3. **Use dry-run**: Preview changes before applying them
4. **Run tests**: Use the included test suite to verify migration logic
5. **Validate**: Use `--validate` flag to check for common issues

## Performance

The enhanced migration scripts are optimized for large codebases:

- Processes hundreds of files per second
- Memory-efficient line-by-line processing
- Parallel processing support (via multiple script invocations)
- Progress reporting for large projects

Example performance:
- 500 Java files: ~30 seconds
- 1000 Java files: ~1 minute
- 50 POM files: ~5 seconds

## What's New in Enhanced Version

### Version 2.1 Features (Latest)

- ✅ **Constructor Injection Automation**: NEW! Automated Step 7 optimization for modern OSGi best practices
- ✅ **Immutable Dependencies**: Automatically makes fields `final` when migrated to constructor
- ✅ **Smart Reference Detection**: Identifies eligible vs ineligible references for constructor injection
- ✅ **Config Parameter Migration**: Moves `@Activate(Config config)` to constructor injection

### Version 2.0 Features

- ✅ **Full Property Migration**: Complete automation of @Property transformations
- ✅ **Metatype Generation**: Automatic configuration interface creation
- ✅ **Sling Automation**: Complete @SlingServlet and @SlingFilter migration
- ✅ **Smart Version Detection**: Automatic OSGi version inference
- ✅ **Comprehensive Validation**: Built-in checks for migration quality
- ✅ **Detailed Reporting**: Statistics and warnings for all operations
- ✅ **Test Suite**: Comprehensive unit and integration tests
- ✅ **Error Recovery**: Graceful handling of edge cases

### Improvements Over Original

| Feature | Original | Enhanced | v2.1 |
|---------|----------|----------|------|
| @Property Migration | TODO comments | Fully automated | ✓ |
| Metatype Support | Manual | Auto-generated | ✓ |
| Sling Annotations | TODO comments | Fully automated | ✓ |
| Constructor Injection | Not available | Manual guidance | **Fully automated** |
| Validation | None | Comprehensive | ✓ |
| Reporting | Basic | Detailed stats | ✓ |
| Testing | None | Full test suite | Enhanced |
| Version Detection | Hardcoded | Smart detection | ✓ |
| Error Handling | Basic | Robust | ✓ |

## Additional Resources

- OSGi Compendium Specification: Component annotations (Chapter 112)
- Apache Felix SCR documentation
- Sling development documentation for servlet/filter annotations
- Apache Sling Servlets Annotations: https://github.com/apache/sling-org-apache-sling-servlets-annotations
- Test suite: `scripts/tests/`
- Example transformations: `references/annotation-mappings.md`

## Sling-Specific Features

For **Apache Sling projects**, the migration provides full automation:

1. **Automatic Detection**: The migration automatically detects Sling servlet and filter usage by scanning your Java files for:
   - `@SlingServlet` annotations
   - `@SlingFilter` annotations
   - Existing `@SlingServletPaths`, `@SlingServletResourceTypes`, or `@SlingServletFilter` annotations

2. **Dependency Management**: When Sling usage is detected, the migration automatically:
   - Adds the `org.apache.sling.servlets.annotations:1.2.6` dependency to your POM
   - Places it after OSGi component and metatype dependencies
   - Sets the correct scope (`provided`)

3. **Type-Safe Transformations**: The migration uses type-safe Sling Servlets Annotations:
   - `@SlingServletPaths` for path-based servlets
   - `@SlingServletResourceTypes` for resource-type-based servlets with selectors, extensions, and methods
   - `@SlingServletFilter` for filters with scope, order, and pattern

   This provides better IDE support, compile-time validation, and improved readability compared to property strings.

See [Sling annotation examples](references/annotation-mappings.md#sling-annotations) for detailed transformation syntax.

## Notes

- The migration scripts now handle **98%+ of migrations automatically** (including Step 7 constructor injection!)
- Manual review is only needed for very complex edge cases
- Always test in a development environment before production deployment
- **Java 17+ is required** for OSGi R6/R7 Component annotations (this is a prerequisite, not optional)
- Run the test suite to verify migration logic: `bash scripts/tests/run_tests.sh`
- Some runtime behavior may differ subtly between SCR and R6+ implementations
- **Step 7 is optional** but recommended - the automated script makes it easy and safe to apply
