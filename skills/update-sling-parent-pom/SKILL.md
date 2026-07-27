---
name: update-sling-parent-pom
description: Use when the user asks to upgrade the Sling parent POM version in their project
---

# Updating the Sling Parent POM

## When to use

Use when the user asks to upgrade the Sling parent POM version in their project. 

## Instructions

- record the current Java and Maven environment with `java -version` and `mvn -version`
- run a baseline `mvn clean verify` if the current checkout is expected to support the current Java version
    - if the baseline build fails only because the current parent POM or project setup requires an older Java version, document that and continue
    - assume the unchanged project would pass with its required Java version; do not switch to version-specific Maven wrappers just to prove the baseline unless the user explicitly asks
    - if the baseline fails for a reason unrelated to the Java version, abort execution and inform the user of the root cause before changing the parent POM
- check if any known workarounds exist, but do not remove them yet
- if other repos in the same workspace/org use the same parent artifact and test patterns (e.g. pax-exam quickstart ITs), check whether one has already been migrated to the target parent version — its pinned test/IT dependency versions are a validated reference point and can save significant troubleshooting
- identify the current version of the Sling parent POM in the user's project
- determine the latest available Sling parent POM version for the applicable parent artifact
    - target the latest available version, not merely the next increment
- check `https://cwiki.apache.org/confluence/spaces/SLING/pages/` for an official "Update to Parent NN" migration page covering a version between the current and target (search or ask the user; these pages are not sequentially numbered). Follow any steps it documents that are not already covered by this skill's reference files
- update the parent POM version
- run `mvn validate` with the current Java version before running a full build
    - if `mvn validate` fails, fix any POM syntax issues first
    - if the new parent POM no longer manages some plugin versions, add explicit plugin versions for the affected plugins
    - if the new parent POM no longer manages some dependency versions, add explicit dependency versions for the affected dependencies
    - do not continue to `clean verify` until `mvn validate` succeeds
- run a full `mvn clean verify` build with the current Java version
    - the goal is for the upgraded project to build successfully with the current Java version
    - if the build fails because Spotless formatting checks were introduced or changed by the new parent POM, run `mvn spotless:apply`, review the resulting formatting changes, and rerun `mvn clean verify`
- if the project's CI runs a Java version matrix (check the Jenkinsfile or CI config; commonly includes versions newer than the one just used to verify), also verify with the **highest** JDK version in that matrix using the appropriate `mvnNN` wrapper (e.g. `mvn21 clean verify` for JDK 21 — see [skills/README.md](../README.md) § Invoking Apache Maven with different Java versions) before considering the upgrade complete — some failures (e.g. [felix-http-jetty-jdk21.md](./references/felix-http-jetty-jdk21.md)) only reproduce on newer JDKs and will not show up when verifying with the minimum required version alone
- apply additional verification steps as needed if the specific version requires it
- if workarounds were present before updating the parent POM version, check if they can be removed. If they are needed add them back

Do not touch the git repository state - no staging/unstaging, no commits, no pushes.

If the user separately commits and pushes the changes (e.g. to an open PR) and a CI build fails, fetch the raw log (e.g. a Jenkins job's `consoleText` endpoint) rather than a JS-rendered log viewer — rendered pipeline-explorer pages often don't expose the actual error text to a fetch tool. A "tests passed" summary alongside an overall build failure means the real failure is in a later stage (verify/IT/checks) — check the full log rather than stopping at the test summary.

### Unsupported versions

The following historical versions are not supported and must be skipped:
- 42
- 50

### Additional fixes

Use the following reference files when a matching migration issue or build failure appears.

- rat-disable: [rat-disable.md](./references/rat-disable.md)
- osgi-r6: [osgi-r6.md](./references/osgi-r6.md)
- servlet-3: [servlet-3.md](./references/servlet-3.md)
- osgi-annotations: [osgi-annotations.md](./references/osgi-annotations.md)
- bundle-parent: [bundle-parent.md](./references/bundle-parent.md)
- timestamp: [timestamp.md](./references/timestamp.md)
- mockito-java17: [mockito-java17.md](./references/mockito-java17.md)
- readd-scr-annotations: [readd-scr-annotations.md](./references/readd-scr-annotations.md)
- osgi-deps: [osgi-deps.md](./references/osgi-deps.md)
- deps-scope: [deps-scope.md](./references/deps-scope.md)
- sling-java-version: [sling-java-version.md](./references/sling-java-version.md)
- slf4j2-paxexam-it: [slf4j2-paxexam-it.md](./references/slf4j2-paxexam-it.md)
- felix-http-jetty-jdk21: [felix-http-jetty-jdk21.md](./references/felix-http-jetty-jdk21.md)
- pax-exam-timeouts: [pax-exam-timeouts.md](./references/pax-exam-timeouts.md)
- git-blame-ignore-revs: [git-blame-ignore-revs.md](./references/git-blame-ignore-revs.md)

### Workarounds

- rat-enable: [rat-enable.md](./references/rat-enable.md)
