# SCR to OSGi R6/R7 Annotation Migration Reference

Comprehensive mapping of deprecated Felix SCR annotations to official OSGi Component annotations.

## Table of Contents
- [Imports](#imports)
- [@Component](#component)
- [@Service](#service)
- [@Reference](#reference)
- [@Property](#property)
- [Lifecycle Annotations](#lifecycle-annotations)
- [Sling Annotations](#sling-annotations)
- [Common Patterns](#common-patterns)

## Imports

### Remove SCR Imports
```java
// OLD - Remove these
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
```

### Add OSGi R6/R7 Imports
```java
// NEW - Add these
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
```

## @Component

### Basic Component

```java
// OLD
@Component
@Service
public class MyService implements SomeInterface {
}

// NEW
@Component(service = SomeInterface.class)
public class MyService implements SomeInterface {
}
```

### Component with Metadata

```java
// OLD
@Component(
    metatype = true,
    label = "My Service",
    description = "This is my service"
)
@Service
public class MyService {
}

// NEW
@Component(
    property = {
        "service.description=My Service"
    }
)
public class MyService {
}
```

Note: OSGi R6+ uses OSGi Metatype annotations separately if configuration is needed:
```java
@Component
@Designate(ocd = MyServiceConfig.class)
public class MyService {
}

@ObjectClassDefinition(
    name = "My Service",
    description = "This is my service"
)
public @interface MyServiceConfig {
    @AttributeDefinition(name = "Property Name")
    String myProperty() default "default";
}
```

### Component with Immediate Activation

```java
// OLD
@Component(immediate = true)

// NEW
@Component(immediate = true)  // Same syntax
```

### Component with Custom Name

```java
// OLD
@Component(name = "com.example.MyService")

// NEW
@Component(name = "com.example.MyService")  // Same syntax
```

## @Service

The `@Service` annotation is removed in OSGi R6. Service registration is now part of `@Component`.

### Single Service

```java
// OLD
@Component
@Service(value = MyInterface.class)
public class MyService implements MyInterface {
}

// NEW
@Component(service = MyInterface.class)
public class MyService implements MyInterface {
}
```

### Multiple Services

```java
// OLD
@Component
@Service(value = {InterfaceA.class, InterfaceB.class})
public class MyService implements InterfaceA, InterfaceB {
}

// NEW
@Component(service = {InterfaceA.class, InterfaceB.class})
public class MyService implements InterfaceA, InterfaceB {
}
```

### Service Without Explicit Interface

```java
// OLD
@Component
@Service
public class MyService implements MyInterface {
}

// NEW - Automatically registers all implemented interfaces
@Component
public class MyService implements MyInterface {
}

// Or explicitly specify
@Component(service = MyInterface.class)
public class MyService implements MyInterface {
}
```

## @Reference

### Simple Reference

```java
// OLD
@Reference
private MyService myService;

// NEW - Same syntax
@Reference
private MyService myService;
```

### Reference with Cardinality

```java
// OLD
@Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
private MyService myService;

// NEW
@Reference(cardinality = ReferenceCardinality.OPTIONAL)
private MyService myService;
```

Cardinality mapping:
- `OPTIONAL_UNARY` → `OPTIONAL` (0..1)
- `MANDATORY_UNARY` → `MANDATORY` (1..1)
- `OPTIONAL_MULTIPLE` → `MULTIPLE` (0..n)
- `MANDATORY_MULTIPLE` → `AT_LEAST_ONE` (1..n)

### Reference with Policy

```java
// OLD
@Reference(policy = ReferencePolicy.DYNAMIC)
private volatile MyService myService;

// NEW - Same syntax
@Reference(policy = ReferencePolicy.DYNAMIC)
private volatile MyService myService;
```

### Reference with Bind/Unbind Methods

```java
// OLD
@Reference(
    bind = "bindService",
    unbind = "unbindService"
)
private MyService myService;

protected void bindService(MyService service) {
    this.myService = service;
}

protected void unbindService(MyService service) {
    this.myService = null;
}

// NEW - Same syntax
@Reference(
    bind = "bindService",
    unbind = "unbindService"
)
private MyService myService;

protected void bindService(MyService service) {
    this.myService = service;
}

protected void unbindService(MyService service) {
    this.myService = null;
}
```

### Reference with Target Filter

```java
// OLD
@Reference(target = "(service.pid=com.example.MyService)")
private MyService myService;

// NEW - Same syntax
@Reference(target = "(service.pid=com.example.MyService)")
private MyService myService;
```

### Reference with Policy Option (New in OSGi R6)

```java
// NEW - Available in OSGi R6+
@Reference(
    policyOption = ReferencePolicyOption.GREEDY
)
private MyService myService;
```

## @Property

Properties must be moved from `@Property` annotations to the `property` attribute of `@Component`.

### Simple Property

```java
// OLD
@Component
@Property(name = "service.vendor", value = "Apache Software Foundation")
public class MyService {
}

// NEW
@Component(
    property = {
        "service.vendor=Apache Software Foundation"
    }
)
public class MyService {
}
```

### Multiple Properties

```java
// OLD
@Component
@Property(name = "prop1", value = "value1")
@Property(name = "prop2", value = "value2")
public class MyService {
}

// NEW
@Component(
    property = {
        "prop1=value1",
        "prop2=value2"
    }
)
public class MyService {
}
```

### Property with Multiple Values

```java
// OLD
@Property(name = "paths", value = {"/path1", "/path2"})

// NEW
@Component(
    property = {
        "paths=/path1",
        "paths=/path2"
    }
)
```

### Boolean Property

```java
// OLD
@Property(name = "enabled", boolValue = true)

// NEW
@Component(
    property = {
        "enabled:Boolean=true"
    }
)
```

### Integer Property

```java
// OLD
@Property(name = "ranking", intValue = 100)

// NEW
@Component(
    property = {
        "service.ranking:Integer=100"
    }
)
```

### Property with Label and Description

```java
// OLD
@Property(
    name = "my.property",
    value = "default",
    label = "My Property",
    description = "This is my property"
)

// NEW - Use Metatype annotations for configuration
@Component
@Designate(ocd = MyConfig.class)
public class MyService {
}

@ObjectClassDefinition(name = "My Service Config")
public @interface MyConfig {
    @AttributeDefinition(
        name = "My Property",
        description = "This is my property"
    )
    String my_property() default "default";
}
```

### Configurable Property

```java
// OLD
@Component(metatype = true)
@Property(name = "my.property", value = "default")
private static final String MY_PROPERTY = "my.property";

@Activate
private void activate(Map<String, Object> properties) {
    String value = PropertiesUtil.toString(properties.get(MY_PROPERTY), "default");
}

// NEW
@Component
@Designate(ocd = Config.class)
public class MyService {

    @ObjectClassDefinition(name = "My Service Configuration")
    public @interface Config {
        String my_property() default "default";
    }

    @Activate
    private void activate(Config config) {
        String value = config.my_property();
    }
}
```

### Configurable Property with Label and Description (Component Property Types)

```java
// OLD - Using Map (deprecated approach)
@Component(metatype = true, label = "My Service", description = "This is my service")
@Property(name = "timeout", intValue = 30, label = "Timeout", description = "Request timeout in seconds")
public class MyService {
    @Activate
    private void activate(Map<String, Object> props) {
        int timeout = PropertiesUtil.toInteger(props.get("timeout"), 30);
    }
}

// NEW - Using Component Property Types (preferred OSGi R7 approach)
// Label, description, and @Activate signature are automatically migrated
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
        // Type-safe access to configuration properties
        int timeout = config.timeout();
    }
}
```

**Benefits of Component Property Types:**
- ✅ Type-safe configuration access
- ✅ No need for type conversion utilities (PropertiesUtil)
- ✅ IDE auto-completion for configuration properties
- ✅ Compile-time validation of property names
- ✅ Default values defined in one place

### Service Property Type Annotations

The `org.osgi.service.component.propertytypes` package provides type-safe annotations for common service properties. The migration tool automatically uses these instead of property strings.

#### @ServiceRanking

```java
// OLD
@Component
@Property(name = "service.ranking", intValue = 100)
public class MyService {
}

// NEW - Automatically migrated
import org.osgi.service.component.propertytypes.ServiceRanking;

@ServiceRanking(100)
@Component
public class MyService {
}
```

#### @ServiceDescription

```java
// OLD
@Component
@Property(name = "service.description", value = "My Service Description")
public class MyService {
}

// NEW - Automatically migrated
import org.osgi.service.component.propertytypes.ServiceDescription;

@ServiceDescription("My Service Description")
@Component
public class MyService {
}
```

#### @ServiceVendor

```java
// OLD
@Component
@Property(name = "service.vendor", value = "Apache Software Foundation")
public class MyService {
}

// NEW - Automatically migrated
import org.osgi.service.component.propertytypes.ServiceVendor;

@ServiceVendor("Apache Software Foundation")
@Component
public class MyService {
}
```

**Advantages of Property Type Annotations:**
- ✅ Type-safe at compile time
- ✅ Better IDE support and auto-completion
- ✅ Cleaner, more readable code
- ✅ Part of the `org.osgi.service.component` artifact (version 1.5.1 required)

**Required Dependency:**
```xml
<dependency>
    <groupId>org.osgi</groupId>
    <artifactId>org.osgi.service.component</artifactId>
    <version>1.5.1</version>
    <scope>provided</scope>
</dependency>
```

Note: This dependency provides the `org.osgi.service.component.propertytypes` package containing `@ServiceRanking`, `@ServiceDescription`, and `@ServiceVendor` annotations.

#### Property Type Annotation Requirements

⚠️ **Important**: Property type annotations require the correct type attribute:

| Annotation | Property Name | Required Type Attribute |
|------------|---------------|------------------------|
| `@ServiceRanking` | service.ranking | `intValue` (not `value`) |
| `@ServiceDescription` | service.description | `value` (String) |
| `@ServiceVendor` | service.vendor | `value` (String) |

**Wrong Type Example**:
```java
// WRONG - will fall back to property string
@Property(name = "service.ranking", value = "100")  // Should be intValue

// CORRECT - will use @ServiceRanking annotation
@Property(name = "service.ranking", intValue = 100)
```

The migration tool will automatically:
1. Use property type annotations when type is correct
2. Fall back to property strings when type is wrong
3. Log a warning for type mismatches

**Example Warning**:
```
⚠ test.java: Property 'service.ranking' has type STRING, expected INTEGER.
  Will use property string instead of @ServiceRanking annotation.
```

## Lifecycle Annotations

### @Activate

```java
// OLD
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;

@Component
public class MyService {
    @Activate
    protected void activate(ComponentContext context) {
        // activation code
    }
}

// NEW
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component
public class MyService {
    @Activate
    protected void activate() {
        // activation code
    }
}
```

Activate method signatures in OSGi R6+:
```java
@Activate
void activate();

@Activate
void activate(ComponentContext context);

@Activate
void activate(BundleContext context);

@Activate
void activate(Map<String, Object> properties);

@Activate
void activate(Config config);  // With @Designate

@Activate
void activate(ComponentContext context, Map<String, Object> properties);
```

### @Deactivate

```java
// OLD
@Deactivate
protected void deactivate(ComponentContext context) {
    // deactivation code
}

// NEW - Same syntax
@Deactivate
protected void deactivate() {
    // deactivation code
}
```

### @Modified

```java
// OLD
import org.apache.felix.scr.annotations.Modified;

@Modified
protected void modified(ComponentContext context) {
    // modification code
}

// NEW
import org.osgi.service.component.annotations.Modified;

@Modified
protected void modified(Map<String, Object> properties) {
    // modification code
}
```

## Sling Annotations

Apache Sling servlet and filter annotations migrate to type-safe **Sling Servlets Annotations** by default.

### Default Approach: Sling Servlets Annotations (Automatic)

The migration automatically uses **org.apache.sling:org.apache.sling.servlets.annotations:1.2.6** which provides OSGi-based annotations specifically designed for Sling servlets and filters. This dependency is automatically added to your POM when Sling usage is detected.

### Alternative Approach: Standard OSGi @Component with Properties

You can also use standard OSGi annotations with Sling-specific property strings, though the type-safe annotations approach is recommended.

**Dependency:**
```xml
<dependency>
    <groupId>org.apache.sling</groupId>
    <artifactId>org.apache.sling.servlets.annotations</artifactId>
    <version>1.2.6</version>
    <scope>provided</scope>
</dependency>
```

### @SlingServlet

#### Default: Sling Servlets Annotations (Automatic)

```java
// OLD
import org.apache.felix.scr.annotations.sling.SlingServlet;

@SlingServlet(
    paths = {"/bin/myservlet"},
    methods = {"GET", "POST"}
)
public class MyServlet extends SlingAllMethodsServlet {
}

// NEW - Using Sling Servlets Annotations (Automatic)
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import javax.servlet.Servlet;

@Component(service = Servlet.class)
@SlingServletPaths(value = {"/bin/myservlet"})
public class MyServlet extends SlingAllMethodsServlet {
    // Methods GET and POST are inherited from SlingAllMethodsServlet
}
```

#### Alternative: Standard OSGi @Component

```java
// OLD
import org.apache.felix.scr.annotations.sling.SlingServlet;

@SlingServlet(
    paths = {"/bin/myservlet"},
    methods = {"GET", "POST"}
)
public class MyServlet extends SlingAllMethodsServlet {
}

// NEW - Using Standard OSGi Annotations
import org.osgi.service.component.annotations.Component;
import javax.servlet.Servlet;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/myservlet",
        "sling.servlet.methods=GET",
        "sling.servlet.methods=POST"
    }
)
public class MyServlet extends SlingAllMethodsServlet {
}
```

### @SlingServlet with Resource Types

#### Default: Sling Servlets Annotations (Automatic)

```java
// OLD
@SlingServlet(
    resourceTypes = {"my/resource/type"},
    selectors = {"print"},
    extensions = {"html", "pdf"}
)

// NEW - Using Sling Servlets Annotations (Automatic)
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import javax.servlet.Servlet;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = "my/resource/type",
    selectors = "print",
    extensions = {"html", "pdf"}
)
```

### @SlingFilter

#### Default: Sling Servlets Annotations (Automatic)

```java
// OLD
import org.apache.felix.scr.annotations.sling.SlingFilter;

@SlingFilter(
    scope = SlingFilterScope.REQUEST,
    order = -700
)
public class MyFilter implements Filter {
}

// NEW - Using Sling Servlets Annotations (Automatic)
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Component;
import javax.servlet.Filter;

@Component(service = Filter.class)
@SlingServletFilter(
    scope = SlingServletFilterScope.REQUEST,
    order = -700
)
public class MyFilter implements Filter {
}
```

#### Alternative: Standard OSGi @Component

```java
// OLD
import org.apache.felix.scr.annotations.sling.SlingFilter;

@SlingFilter(
    scope = SlingFilterScope.REQUEST,
    order = -700
)
public class MyFilter implements Filter {
}

// NEW - Using Standard OSGi Annotations
import org.osgi.service.component.annotations.Component;
import javax.servlet.Filter;

@Component(
    service = Filter.class,
    property = {
        "sling.filter.scope=REQUEST",
        "service.ranking:Integer=-700"
    }
)
public class MyFilter implements Filter {
}
```

### Sling Annotations Comparison

| Feature | Standard OSGi @Component | Sling Servlets Annotations |
|---------|-------------------------|----------------------------|
| Dependency | None (standard OSGi) | org.apache.sling:org.apache.sling.servlets.annotations:1.2.6 |
| Syntax | Properties in @Component | Dedicated annotations |
| Type Safety | String properties | Type-safe annotation attributes |
| Readability | Verbose property strings | Cleaner, more readable |
| Migration Default | Alternative approach | **Default** (automatically applied) |
| Recommendation | Works everywhere | Recommended for Sling projects |

**Note:** The migration tool automatically uses Sling Servlets Annotations (version 1.2.6) when Sling servlets or filters are detected. These annotations are built on top of OSGi R6/R7 and provide a cleaner, more type-safe API for Sling servlet and filter registration.

## Common Patterns

### Pattern 1: Basic Service with Configuration

```java
// OLD
@Component(
    metatype = true,
    label = "My Service",
    immediate = true
)
@Service
@Property(name = "timeout", intValue = 30)
public class MyService implements SomeInterface {
    private int timeout;

    @Activate
    protected void activate(Map<String, Object> props) {
        timeout = PropertiesUtil.toInteger(props.get("timeout"), 30);
    }
}

// NEW
@Component(
    service = SomeInterface.class,
    immediate = true
)
@Designate(ocd = MyService.Config.class)
public class MyService implements SomeInterface {

    @ObjectClassDefinition(name = "My Service")
    public @interface Config {
        @AttributeDefinition(name = "Timeout")
        int timeout() default 30;
    }

    @Activate
    protected void activate(Config config) {
        int timeout = config.timeout();
    }
}
```

### Pattern 2: Service with Multiple References

```java
// OLD
@Component
@Service
public class MyService implements SomeInterface {

    @Reference
    private ServiceA serviceA;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private ServiceB serviceB;

    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile ServiceC serviceC;
}

// NEW - Same syntax for references
@Component(service = SomeInterface.class)
public class MyService implements SomeInterface {

    @Reference
    private ServiceA serviceA;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private ServiceB serviceB;

    @Reference(policy = ReferencePolicy.DYNAMIC)
    private volatile ServiceC serviceC;
}
```

### Pattern 3: Factory Component

```java
// OLD
@Component(
    factory = "my.service.factory",
    metatype = false
)
@Service
public class MyService {
}

// NEW
@Component(
    factory = "my.service.factory"
)
public class MyService {
}
```

### Pattern 4: Service Ranking (Component Property Types)

```java
// OLD
@Component
@Service
@Property(name = "service.ranking", intValue = 100)
public class MyService {
}

// NEW - Using Component Property Type Annotation (Preferred)
import org.osgi.service.component.propertytypes.ServiceRanking;

@ServiceRanking(100)
@Component
public class MyService {
}

// Alternative - Using property strings
@Component(
    property = {
        "service.ranking:Integer=100"
    }
)
public class MyService {
}
```

**Note:** The migration tool automatically uses Component Property Type annotations (`@ServiceRanking`, `@ServiceDescription`, `@ServiceVendor`) for standard service properties, providing better type-safety and IDE support.

### Pattern 5: Constructor Injection for Configuration (Optimized - OSGi R7+ Best Practice)

```java
// After basic migration - Field injection with @Activate method
@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    @ObjectClassDefinition(name = "My Service")
    public @interface Config {
        @AttributeDefinition(name = "Timeout")
        int timeout() default 30;
    }

    private int timeout;

    @Activate
    private void activate(Config config) {
        this.timeout = config.timeout();
    }
}

// After optimization - Constructor injection (recommended)
@Component
@Designate(ocd = MyService.Config.class)
public class MyService {

    @ObjectClassDefinition(name = "My Service")
    public @interface Config {
        @AttributeDefinition(name = "Timeout")
        int timeout() default 30;
    }

    private final int timeout;

    @Activate
    public MyService(Config config) {
        this.timeout = config.timeout();
    }
}
```

**Benefits:**
- ✅ Configuration fields are `final` (immutable)
- ✅ Constructor must be `public` (explicit visibility)
- ✅ Configuration available immediately upon construction
- ✅ Better testability with constructor parameters

### Pattern 6: Constructor Injection for References (Optimized - OSGi R7+ Best Practice)

```java
// After basic migration - Field injection
@Component(service = MyService.class)
public class MyService {

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private SlingRepository repository;

    public void doWork() {
        // Use services
    }
}

// After optimization - Constructor injection for mandatory static references
@Component(service = MyService.class)
public class MyService {

    private final ResourceResolverFactory resolverFactory;
    private final SlingRepository repository;

    @Activate
    public MyService(
        @Reference ResourceResolverFactory resolverFactory,
        @Reference SlingRepository repository
    ) {
        this.resolverFactory = resolverFactory;
        this.repository = repository;
    }

    public void doWork() {
        // Use services (guaranteed non-null)
    }
}
```

**When to use constructor injection for references:**
- ✅ Mandatory references (default or `cardinality = MANDATORY`)
- ✅ Static references (default or `policy = STATIC`)
- ✅ Unary references (single service)

**When to keep field injection:**
- ❌ Optional references (`cardinality = OPTIONAL`)
- ❌ Dynamic references (`policy = DYNAMIC`)
- ❌ Multiple references (`cardinality = MULTIPLE` or `AT_LEAST_ONE`)

### Pattern 7: Combined Constructor Injection (Optimized - OSGi R7+ Best Practice)

```java
// Optimized: Configuration + mandatory references in constructor
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

    // Optional references stay as field injection
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private ConfigurationAdmin configAdmin;

    @Activate
    public MyService(
        Config config,  // Config must be first parameter
        @Reference ResourceResolverFactory resolverFactory
    ) {
        this.timeout = config.timeout();
        this.resolverFactory = resolverFactory;
    }
}
```

**Important:**
- Configuration parameter must be **first** in constructor signature
- Constructor must be `public` for OSGi dependency injection
- Use `@Reference` annotation on each reference parameter
- Mix field injection for optional/dynamic references with constructor injection for mandatory static references
