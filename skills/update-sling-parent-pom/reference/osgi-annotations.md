# OSGi Annotations

Replace usage of the deprecated Apache Felix SCR annotations with the official OSGi annotations.

## pom.xml

Remove the following plugins:
- org.apache.felix:maven-scr-plugin 

Remove the following dependencies:
- org.apache.felix:org.apache.felix.scr.annotations


## Java imports

Imports to remove

```java
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
```

Imports to add

```java
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
```

## The @Service annotation

The `@Service` annotation is no longer needed. Instead, the `service` attribute of the `@Component` annotation should be used to specify the service interfaces.

Before:

``java
@Component
@Service(MyService.class)
```

After

```java
@Component(service = MyService.class)
```

## The @Property annotation for a top-level class

The `@Property` annotation is replaced by the 'property' attribute of the `@Component` annotation.

Before:

```java
@Component
@Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling MIME Type Service")
```

After:

```java
@Component
    property = {
            Constants.SERVICE_DESCRIPTION + "=Apache Sling MIME Type Service"
    })
```

## The @Properties annotation

The `@Properties` annotation is replaced by the 'property' attribute of the `@Component` annotation.

Before:
```java
@Component
@Properties({
    @Property(name = "sling.servlet.extensions", value = "json"),
    @Property(name = "sling.servlet.resourceTypes", value = "sling/bar")
})
```

After:
```java
@Component(
    property = {
        "sling.servlet.extensions=json",
        "sling.servlet.resourceTypes=sling/bar"
    }
)
```

## The ReferenceCardinality enum

The enum values have changed:

- OPTIONAL_UNARY → OPTIONAL
- OPTIONAL_MULTIPLE → MULTIPLE
