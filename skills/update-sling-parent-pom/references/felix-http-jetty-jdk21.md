# pax-exam quickstart fails to start under JDK 21 (Unsupported class file major version 65)

If a pax-exam IT using `SlingOptions.slingQuickstartOakTar()` (or similar quickstart helpers) fails only on newer JDKs (observed on JDK 21), the JUnit-visible symptom is misleading — it looks like a slow-startup/timeout problem:

```
org.ops4j.pax.swissbox.tracker.ServiceLookupException: gave up waiting for service org.apache.sling.engine.SlingRequestProcessor
```

Increasing the service-lookup timeout (see [pax-exam-timeouts.md](./pax-exam-timeouts.md)) does not fix this — the real error is buried earlier in the container log (`target/failsafe-reports/<TestClass>-output.txt`, not the summary `.txt`):

```
ERROR: Bundle org.apache.felix.http.jetty [..] Error starting ... (org.osgi.framework.BundleException: Activator start error ...)
Caused by: java.lang.IllegalArgumentException: Unsupported class file major version 65
```

Class file major version 65 is Java 21 bytecode. The quickstart's default `org.apache.felix.http.jetty` (5.0.0) bundles an ASM version too old to parse Java 21 classes during bytecode analysis. When the bundle fails to activate, every service the Sling engine provides — including `SlingRequestProcessor` — never comes up.

## Fix

Pin the affected bundles back and bump ASM via `SlingOptions.versionResolver`, in the test-support class's `baseConfiguration()` (or equivalent), before building the pax-exam options:

```java
SlingOptions.versionResolver.setVersion("org.apache.felix", "org.apache.felix.http.jetty", "4.2.0");
SlingOptions.versionResolver.setVersion("org.apache.felix", "org.apache.felix.http.servlet-api", "2.0.0");
SlingOptions.versionResolver.setVersion("org.ow2.asm", "asm", "9.7.1");
SlingOptions.versionResolver.setVersion("org.ow2.asm", "asm-analysis", "9.7.1");
SlingOptions.versionResolver.setVersion("org.ow2.asm", "asm-commons", "9.7.1");
SlingOptions.versionResolver.setVersion("org.ow2.asm", "asm-tree", "9.7.1");
SlingOptions.versionResolver.setVersion("org.ow2.asm", "asm-util", "9.7.1");
```

This exact override is already used by `sling-org-apache-sling-graphql-core` — check other pax-exam-based sibling repos for the same pattern before troubleshooting from scratch.

## Verifying the fix

This only reproduces on JDK 21 (JDK 17 quickstart ITs pass fine even without the fix). Since the project's CI runs a JDK matrix (commonly 17 and 21, sometimes 11 too — check the Jenkinsfile/CI job), running `mvn clean verify` locally with only the lowest supported JDK is not sufficient to catch this. Verify with the **highest** JDK version the CI matrix uses before pushing.
