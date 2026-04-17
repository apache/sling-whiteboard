# Apache Sling Agent Skills

A set of [Agent Skills](https://agentskills.io/) for Apache Sling developers and users.

Current skills:

- `update-sling-parent-pom`: Updates the parent POM of a Sling project to the next version.

## Prerequisites

First of all, install and configure a coding assistant that supports Agent Skills.

### Invoking Apache Maven with different Java versions

The `update-sling-parent-pom` skill requires running Apache Maven with older JVM versions. Therefore it expects the following commands to exist:

- `mvn8`: Apache Maven running with Java 8
- `mvn11`: Apache Maven running with Java 11
- `mvn17`: Apache Maven running with Java 17
- `mvn21`: Apache Maven running with Java 21

There is no generic solution for creating these scripts. One approach is to create small wrapper scripts and add them to PATH. For example, `mvn11` would be:

```bash
#!/bin/sh -e

JAVA_HOME=/usr/lib64/jvm/java-11
export JAVA_HOME

mvn "$@"
```

### Generating diffs for for OSGi components

Some parent pom updates require changes to the OSGi components descriptors. These can be validated using the [osgi-ds-metatype-diff](https://github.com/jsedding/osgi-ds-metatype-diff) tool. Since there is no release of this tool yet you will need to build it and install it to your local Maven repository. Building using `-DskipTests -DskipITs` is required for recent Java versions.

This dependency is optional but recommended.

## Installation

Using [npx skills](https://github.com/vercel-labs/skills):

```bash
    npx skills add apache/sling-whiteboard
```

Alternatively, clone the repository and copy the skill to a locations supported by your coding assistant.
