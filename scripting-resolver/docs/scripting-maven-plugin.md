Apache Sling Scripting Maven Plugin (WIP, name not final)
====

## What

This plugin provides support for deriving _sling.resourceType_ Requirements and Capabilities from
the filesystem layout of scripts. Bundles that get extended by the
[`org.apache.sling.scripting.resolver`](./scripting-resolver.md) with these Requirements and
Capabilities will have their scripts made available automatically with added versioning and dependency
support.

## Why

Manually defining the Requirements and Capabilities is error-prone and unnecessary as they can be
derived from the filesystem layout required for scripts by the _resolver_ (for the most part).

## How

The plugin scans the javax.script directory and will generate Capabilities for scripts it finds. It
expects the following layout:

```
javax.script/<sling-resourceType>/<version>/[<METHOD>.]<name>[.<selector>][.<extension>].<script-extension>
```

From that, it will create a Capability with the matching attributes with the following extra assumptions:

* If the _name_ equals the _sling-resourceType_or the last part of it, it is assumed to be the name of the main script.
* Otherwise, it is assumed to be part of the selector.
* As a special case, if the filename is just _extends_ it is assumed to be a file containing a single line with the
  resourceType used for the _extends_ attribute followed by a _;version=<version-range>_. In this case, the plugin will
  set the _extends_ attribute to the given resourceType and generate a Requirement for that resourcetype in the given
  version range.
* As another special case, if the filename is just _requires_ the plugin will read it line by line (which should each
  look like in the extends case) and generate a requirement.

### Example

As an example, lets assume the following layout:

```
javax.scripting/
    org.foo/1.0.0
        POST.foo.html
```

which will generate something like the following Capability:

```
sling.resourceType;
    sling.resourceType="org.foo";
    sling.servlet.methods:List<String>=POST;
    version:Version="1.0.0"
```

For a bigger example providing several versions and using an extends consider the following two projects: 
 [example](../org-apache-sling-scripting-examplebundle) and
  [example.hi](../org-apache-sling-scripting-examplebundle/pom.xml).

## So how do I use the plugin

Do to lack of time the plugin doesn't integrate with the maven-bundle-plugin atm. The generated Requirements and 
Capabilities are simply made available via properties namely, 

```
${org.apache.sling.scripting.maven.plugin.Provide-Capability}
${org.apache.sling.scripting.maven.plugin.Require-Capability}
```

However, that makes it reasonable straight forward to use the plugin by just adding it into your build in the 
prepare-package phase and use the two properties in the manifest writing instructions of another plugin like e.g. 
(and that probably is the main use-case for now) the maven-bundle-plugin - like this:

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

You can find an example in [here](../org-apache-sling-scripting-examplebundle/pom.xml).