## Migrate to new Sling Parent pom location

Starting with version 35 the coordinates of the Sling parent pom have changed to org.apache.sling:sling-bundle-parent for OSGi bundles.

Projects that are not OSGi bundles can continue using the old coordinates.

## pom.xml changes for bundle projects

Remove the `<packaging>bundle</packaging>` tag.

Replace `org.apache.sling:maven-sling-plugin` with `org.apache.sling:sling-maven-plugin`

Remove the `org.apache.felix:maven-bundle-plugin` completely from the pom.xml file. If a the plugin had a `configuration/instructions` tag move it to a bnd configuration file named `bnd.bnd` in the project root. To migrate to the bnd.bnd file, use the following example as a template:

**pom.xml snippet:**

```xml
<configuration>
  <instructions>
    <Import-Package>
      !org.apache.sling.scripting.core.impl.helper,
      *
    </Import-Package>
    <Provide-Capability>
      osgi.service;objectClass=java.lang.Runnable,
      osgi.service;objectClass=javax.servlet.Servlet,
      osgi.service;objectClass=org.apache.sling.api.adapter.AdapterFactory,
      osgi.service;objectClass=org.apache.sling.models.factory.ModelFactory
    </Provide-Capability>
  </instructions>
</configuration>

```

***bnd.bnd snippet:***
Import-Package:\
    !org.apache.sling.scripting.core.impl.helper,\
    *
Provide-Capability:\
    osgi.service;objectClass=java.lang.Runnable,\
    osgi.service;objectClass=jakarta.servlet.Servlet,\
    osgi.service;objectClass=org.apache.sling.api.adapter.AdapterFactory,\
    osgi.service;objectClass=org.apache.sling.models.factory.ModelFactory
```

If the build fails with a baselining error then add a pom property named `bnd.baseline.skip` with value `true`. This happens for projects that have not yet been released. Example below:

```
[ERROR] Failed to execute goal biz.aQute.bnd:bnd-baseline-maven-plugin:4.2.0:baseline (bnd-baseline) on project org.apache.sling.jcr.js.nodetypes: An error occurred while calculating the baseline: Unable to locate a previous version of the artifact -> [Help 1]
```

