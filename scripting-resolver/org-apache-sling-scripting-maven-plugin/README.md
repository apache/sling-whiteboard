Apache Sling Scripting Maven Plugin (WIP, name not final)
====

## What
This plugin provides support for deriving `sling.resourceType` `Requirements` and `Capabilities` from
the file-system layout of scripts. Bundles that get extended by the
[`org.apache.sling.scripting.resolver`](../org-apache-sling-scripting-resolver) with these `Requirements` and
`Capabilities` will have their scripts made available automatically with added versioning and dependency
support.

## Why
Manually defining the `Require-Capability` and `Provide-Capability` bundle headers is error-prone and unnecessary,
as they can be derived from the file-system layout required for scripts by the resolver (for the most part).

## How
The plugin scans the `javax.script` file-system directory and will generate `Capabilities` for the scripts it finds. It
expects the following layout:

```
javax.script/<sling-resourceType>/<version>/[<METHOD>.]<name>[.<selector>][.<extension>].<script-extension>
```

From that, it will create an appropriate `Provide-Capability` (see the ["How"](../org-apache-sling-scripting-resolver/#how)
section of the scripting resolver) with the following extra assumptions:

  * if the `name` part equals the `sling.resourceType` or the last part of it, it is assumed to be the name of the main script;
  otherwise, it is assumed to be a selector;
  * if the file name is just `extends` it is assumed to be a file containing a single line with the
  `resourceType` used for the `extends` capability attribute followed by a `;version=<version-range>`; in this case, the
  plugin will set the `extends` attribute to the given `resourceType` and generate a `Require-Capability` for that
  `resourceType` with the given version range;
  * if the file name is just `requires` the plugin will read it line by line (assuming the same syntax as for the `extends` file)
  and generate a `Require-Capability` for each line based on the given `resourceType` and version range.

### Example
As an example, let's assume the following layout:

```
javax.scripting/
    org.foo/1.0.0
        POST.foo.html
```

This will generate following `Provide-Capability`:

```
sling.resourceType;
    sling.resourceType="org.foo";
    sling.servlet.methods:List<String>=POST;
    version:Version="1.0.0"
```

For a bigger example providing several versions and using an `extends` file consider the following two projects: 

  * [example](../examples/org-apache-sling-scripting-examplebundle);
  * [example.hi](../examples/org-apache-sling-scripting-examplebundle.hi).

## So how do I use the plugin?

The plugin doesn't currently integrate with the `maven-bundle-plugin`. The generated `Require-Capability` and 
`Provide-Capability` headers values are simply made available via properties:

```
${org.apache.sling.scripting.maven.plugin.Provide-Capability}
${org.apache.sling.scripting.maven.plugin.Require-Capability}
```

However, that makes it reasonable straightforward to use the plugin by just adding it into your build in the 
`prepare-package` phase and use the two properties in the manifest writing instructions of another plugin like 
the `maven-bundle-plugin`:

```
    <plugin>
    <groupId>org.apache.sling</groupId>
        <artifactId>org.apache.sling.scripting.maven.plugin</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>prepare-package</phase>
                <goals>
                    <goal>metadata</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    <plugin>
        <groupId>org.apache.felix</groupId>
        <artifactId>maven-bundle-plugin</artifactId>
        <extensions>true</extensions>
        <configuration>
            <instructions>
                <Provide-Capability>
                    ${org.apache.sling.scripting.maven.plugin.Provide-Capability}
                </Provide-Capability>
                <Require-Capability>
                    osgi.extender;filter:="(&amp;(osgi.extender=sling.scripting)(version>=1.0.0)(!(version>=2.0.0)))",
                    ${org.apache.sling.scripting.maven.plugin.Require-Capability}
                </Require-Capability>
            </instructions>
        </configuration>
    </plugin>
```

You can find an example in [here](../examples/org-apache-sling-scripting-examplebundle/pom.xml).
