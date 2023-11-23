[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

# Apache Sling Bnd Provider Type Checker

This module contains a [Bnd plugin](https://bnd.bndtools.org/chapters/870-plugins.html) enforcing that no class of the current bundle extends or implements a provider type. Note that *using* a provider type (without implementing or extending it) is still allowed (even for consumers).

That ensures that the `import-package` version ranges are not narrow but [broad](https://docs.osgi.org/whitepaper/semantic-versioning/060-importer-policy.html) and the risk that the bundle is incompatible with newer versions of its dependent bundles is less likely.

# Usage

For usage with Maven the Bnd plugin has to be added to the plugin dependencies of either `bnd-maven-plugin` or `maven-bundle-plugin` like this:

```
<plugin>
    <groupId>biz.aQute.bnd</groupId>
    <artifactId>bnd-maven-plugin</artifactId>
    <extensions>true</extensions>
    <dependencies>
        <dependency>
            <groupId>org.apache.sling</groupId>
            <artifactId>org.apache.sling.bnd.providertype</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

In addition the `bnd.bnd` file needs to register the Bnd plugin with the [plugin instruction](https://bnd.bndtools.org/instructions/plugin.html)

```
-plugin.providertype=org.apache.sling.bnd.providertype.ProviderTypeScanner
```

## Prerequisites

Bnd 6.0 or newer (integrated in `bnd-maven-plugin` version 6.0.0+ or `maven-bundle-plugin` version 5.1.5+)

# Provider Type Information

The information whether a type (i.e. a class or interface) is designed as provider or consumer is determined originally from the the annotations [`@org.osgi.annotation.versioning.ProviderType`](https://docs.osgi.org/javadoc/osgi.annotation/8.0.0/org/osgi/annotation/versioning/ProviderType.html) or [`@org.osgi.annotation.versioning.ConsumerType`](https://docs.osgi.org/javadoc/osgi.annotation/8.0.0/org/osgi/annotation/versioning/ConsumerType.html).
In order to speed up the check [the annotation is evaluated and extracted into a dedicated JSON file named `META-INF/api-info.json` when generating the apis jar](https://issues.apache.org/jira/browse/SLING-12135) and being looked up from there within this plugin.