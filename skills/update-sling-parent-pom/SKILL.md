---
name: update-sling-parent-pom
description: Use when the user asks to upgrade the Sling parent POM version in their project
---

# Updating the Sling Parent POM

## When to use

Use when the user asks to upgrade the Sling parent POM version in their project. 

## Instructions


- run a full Maven build with the `clean verify`. Use the Java version required by the current parent pom version. If the build fails  abort execution and inform the user of the root cause. Do not proceeed, otherwise it is impossible to validate the parent pom upgrade.
- check if any known issues with workarounds exist but to do not remove them
- identify the current version of the Sling parent POM in the user's project
- determine the next available version, typically incrementing by 1. For example, if the version is '22' the next version would be '23'
- update the parent pom version
- run a full Maven build with the `clean verify` goals using the Java version required by the new parent pom version.
    - if the new parent pom version supports multiple Java versions, use the highest supported one. For example, out of 8, 11, and 17, use Java 17.
- apply additional verification steps as needed if the specific version requires it
- if workarounds were present before updating the parent pom version, check if they can be removed. If they are needed add them back

Do not touch the git repository state - no staging/unstaging, no commits, no pushes.

### Java versions

Different parent pom versions require different Java versions. This is listed in the table below. If building with a certain Java version is needed, use the following commands:

- Java 8: `mvn8`
- Java 11: `mvn11`
- Java 17: `mvn17`
- Java 21: `mvn21`

The following versions are not supported and must be skipped:
- 42
- 50

| Parent POM Version | Java Version(s) | Additional fixes needed |
|--------------------|-----------------|-------------------------|
| 22                 | 8               | None                    |
| 23                 | 8               | rat-disable             |
| 24                 | 8               | None                    |
| 25                 | 8               | None                    |
| 26                 | 8               | None                    |
| 27                 | 8               | osgi-r6,servlet-3       |
| 28                 | 8               | None                    |
| 29                 | 8               | None                    |
| 30                 | 8               | None                    |
| 31                 | 8               | osgi-annotations        |
| 32                 | 8               | None                    |
| 33                 | 8               | None                    |
| 34                 | 8               | None                    |
| 35                 | 8,11            | bundle-parent           |
| 36                 | 8,11            | None                    |
| 37                 | 8,11            | None                    |
| 38                 | 8,11            | None                    |
| 39                 | 8,11            | None                    |
| 40                 | 8,11            | None                    |
| 41                 | 8,11            | None                    |
| 43                 | 8,11            | None                    |
| 44                 | 8,11            | timestamp               |
| 45                 | 8,11            | None                    |
| 46                 | 8,11,17         | mockito-java17          |
| 47                 | 8,11,17         | None                    |
| 48                 | 8,11,17         | None                    |
| 49                 | 8,11,17         | osgi-deps, deps-scope   |
| 51                 | 8,11,17         | None                    |
| 52                 | 8,11,17         | None                    |


### Additional fixes

See the following reference files for additional fixes needed to update to a specific parent pom version.

- rat-disable: [rat-disable.md](./reference/rat-disable.md)
- osgi-r6: [osgi-r6.md](./reference/osgi-r6.md)
- servlet-3: [servlet-3.md](./reference/servlet-3.md)
- osgi-annotations: [osgi-annotations.md](./reference/osgi-annotations.md)
- bundle-parent: [bundle-parent.md](./reference/bundle-parent.md)
- timestamp: [timestamp.md](./reference/timestamp.md)
- mockito-java17: [mockito-java17.md](./reference/mockito-java17.md)
- osgi-deps: [osgi-deps.md](./reference/osgi-deps.md)
- deps-scope: [deps-scope.md](./reference/deps-scope.md)

### Workarounds

- rat-enable: [rat-enable.md](./reference/rat-enable.md)
