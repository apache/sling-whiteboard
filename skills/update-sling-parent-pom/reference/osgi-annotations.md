# OSGi Annotations

Replace deprecated Apache Felix SCR annotations with the OSGi Declarative Services and OSGi Metatype annotations.

## pom.xml

Remove:

- `org.apache.felix:maven-scr-plugin`
- `org.apache.felix:org.apache.felix.scr.annotations`

## Imports

Remove Felix SCR imports such as:

```java
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
```

Add OSGi imports as needed:

```java
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
```

## Annotation mapping

Use this mapping:

- `@Component` -> `@Component`
- `@Service` -> `@Component(service = ...)`
- `@Reference` -> `@Reference`
- `@Activate` -> `@Activate`
- `@Deactivate` -> `@Deactivate`
- `@Modified` -> `@Modified`
- `@Property` and `@Properties` -> typed configuration annotation plus OSGi metatype annotations; use `@Component(property = ...)` only for simple fixed service properties

## `@Component`

Replace the Felix SCR annotation with `org.osgi.service.component.annotations.Component`.

```java
@Component
public class MyComponent {
}
```

## `@Service`

Remove `@Service`.

List provided service interfaces on `@Component(service = ...)`.

Before:

```java
@Component
@Service(MyService.class)
public class MyComponent implements MyService {
}
```

After:

```java
@Component(service = MyService.class)
public class MyComponent implements MyService {
}
```

If the component must not register a service, set `service = {}` explicitly.

Before:

```java
@Component
public class MyComponent {
}
```

After:

```java
@Component(service = {})
public class MyComponent {
}
```

Always make the intended service registration explicit.

## `@Reference`

Field references can usually be migrated directly.

Before:

```java
@Reference
private EventAdmin eventAdmin;
```

After:

```java
@Reference
private EventAdmin eventAdmin;
```

For method-based references, place `@Reference` on the bind method.

Before:

```java
@Reference(bind = "bindEventAdmin", unbind = "unbindEventAdmin")
private EventAdmin eventAdmin;

protected void bindEventAdmin(final EventAdmin service) {
    this.eventAdmin = service;
}

protected void unbindEventAdmin(final EventAdmin service) {
    if (this.eventAdmin == service) {
        this.eventAdmin = null;
    }
}
```

After:

```java
private EventAdmin eventAdmin;

@Reference(unbind = "unbindEventAdmin")
protected void bindEventAdmin(final EventAdmin service) {
    this.eventAdmin = service;
}

protected void unbindEventAdmin(final EventAdmin service) {
    if (this.eventAdmin == service) {
        this.eventAdmin = null;
    }
}
```

Retain existing reference options when present, for example cardinality and policy.

## Lifecycle annotations

Migrate lifecycle methods one-to-one.

Before:

```java
@Activate
protected void activate() {
}

@Modified
protected void modified() {
}

@Deactivate
protected void deactivate() {
}
```

After:

```java
@Activate
protected void activate() {
}

@Modified
protected void modified() {
}

@Deactivate
protected void deactivate() {
}
```

When migrating configuration-heavy components, prefer typed configuration parameters on lifecycle methods.

```java
@Activate
protected void activate(final MyComponentConfig config) {
}

@Modified
protected void modified(final MyComponentConfig config) {
}
```

## Replacing `@Property` and `@Properties`

Use one of these approaches.

Use `@Component(property = ...)` for simple fixed service properties.

Before:

```java
@Component
@Properties({
    @Property(name = "sling.servlet.extensions", value = "json"),
    @Property(name = "sling.servlet.resourceTypes", value = "sling/bar")
})
public class MyServlet {
}
```

After:

```java
@Component(
    property = {
        "sling.servlet.extensions=json",
        "sling.servlet.resourceTypes=sling/bar"
    }
)
public class MyServlet {
}
```

Use a typed configuration annotation for component configuration formerly described with `@Property`.

```java
@ObjectClassDefinition(
    name = "My Component Configuration",
    description = "Configuration for MyComponent"
)
public @interface MyComponentConfig {

    @AttributeDefinition(name = "Welcome Message")
    String welcome_message() default "Hello World!";

    @AttributeDefinition(name = "Welcome Count")
    int welcome_count() default 3;

    @AttributeDefinition(name = "Output Goodbye")
    boolean output_goodbye() default true;
}
```

Bind that configuration to the component with `@Designate` and consume it in `@Activate` and `@Modified`.

```java
@Component(service = {})
@Designate(ocd = MyComponentConfig.class)
public class MyComponent {

    @Activate
    protected void activate(final MyComponentConfig config) {
    }

    @Modified
    protected void modified(final MyComponentConfig config) {
    }
}
```

Property name mapping for typed configuration follows OSGi rules. In particular, `_` in an annotation method name maps to `.` in the configuration property name.

Example:

```java
String service_ranking() default "100";
```

maps to the configuration property `service.ranking`.

## Metatype annotations

Use these OSGi metatype annotations when the old Felix SCR `@Property` metadata was used to describe editable configuration:

- `@ObjectClassDefinition` on the configuration annotation
- `@AttributeDefinition` on configuration properties
- `@Designate(ocd = ...)` on the component class

## Enum value changes

Update enum values where needed.

`ReferenceCardinality`:

- `OPTIONAL_UNARY` -> `OPTIONAL`
- `OPTIONAL_MULTIPLE` -> `MULTIPLE`
- `MANDATORY_UNARY` -> `MANDATORY`
- `MANDATORY_MULTIPLE` -> `AT_LEAST_ONE`

Keep `ReferencePolicy.STATIC` and `ReferencePolicy.DYNAMIC` unchanged when already specified.

## Migration checklist

For each migrated component:

1. Remove Felix SCR imports.
2. Replace `@Service` with `@Component(service = ...)`.
3. Add `service = {}` for components that should not register a service.
4. Keep field `@Reference` injections or move method-based references to annotations on bind methods.
5. Replace Felix lifecycle annotations with the OSGi lifecycle annotations.
6. Replace simple fixed `@Property` and `@Properties` usage with `@Component(property = ...)`.
7. Replace configurable `@Property` usage with a typed configuration annotation using `@ObjectClassDefinition`, `@AttributeDefinition`, and `@Designate`.
8. Update changed `ReferenceCardinality` enum values.
