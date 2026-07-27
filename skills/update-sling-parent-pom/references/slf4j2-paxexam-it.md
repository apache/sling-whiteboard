# slf4j 2.x vs pax-exam integration tests

Newer Sling parent POM versions manage `slf4j.version` to a 2.x release. If the project's `slf4j-api` dependency has no explicit version (relying on the parent), it silently floats to 2.x at compile time. This breaks two things:

1. **Unit tests**: an older pinned `logback-classic` (e.g. 1.2.3, built against slf4j 1.7.x) can't bind to slf4j-api 2.x. Symptom:
   ```
   java.lang.ClassCastException: class org.slf4j.helpers.NOPLogger cannot be cast to class ch.qos.logback.classic.Logger
   ```
   (SLF4J falls back to a no-op logger because the old logback binding is ignored as incompatible.)

2. **Integration tests using `org.apache.sling.testing.paxexam`'s `slingQuickstartOakTar()`**: the bundle now compiles with `Import-Package: org.slf4j;version="[2.0,3)"`, but the Sling quickstart bundle set includes an older `pax-logging-api` that only **exports** `org.slf4j` at 1.7.x. The OSGi container fails to resolve the bundle:
   ```
   ERROR: Bundle ... missing requirement ... osgi.wiring.package; (&(osgi.wiring.package=org.slf4j)(version>=2.0.0)(!(version>=3.0.0)))
   ```

## Do not chase this by upgrading pax-exam infrastructure

It is tempting to "fix forward" by bumping `org.apache.sling.testing.paxexam`, `org.ops4j.pax.exam`, and `org.apache.felix.framework` to versions whose quickstart bundles a newer `pax-logging-api`/`slf4j-api`. **Avoid this** — each of those major-version bumps drags in unrelated breaking changes:
- **`tinybundles` package renamed**: `org.ops4j.pax.tinybundles.core.TinyBundle` → `org.ops4j.pax.tinybundles.TinyBundle`, `.set()`/`.add()` → `.setHeader()`/`.addResource()`
- **`pax-exam-cm` version must be bumped in lockstep** with `org.ops4j.pax.exam.version` — mismatches throw `NoClassDefFoundError` for tinybundles classes during container bootstrap
- **Probe-bundle classloading regressions** for classes pulled from non-OSGi-exported packages (e.g. `org.apache.sling.servlethelpers.internalrequests.SlingInternalRequest`, which `org.apache.sling.servlet-helpers` never actually `Export-Package`s)

Chasing these one at a time burns many build cycles for a problem that has a much smaller fix.

## Correct fix: pin slf4j-api and logback-classic back

Explicitly pin `slf4j-api` to the version the project used **before** the parent upgrade (do not let it inherit the parent's managed 2.x version), and keep `logback-classic` at its matching pre-upgrade version:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>1.7.36</version>
    <scope>provided</scope>
</dependency>
```

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.3</version>
    <scope>test</scope>
</dependency>
```

This keeps the bundle's `Import-Package: org.slf4j` range at 1.7.x, matching what the pax-exam quickstart's `pax-logging-api` exports — no pax-exam/felix/tinybundles version changes needed.

## If a sibling repo already migrated

If other repos in the same org/workspace use the same pax-exam quickstart IT pattern (`slingQuickstartOakTar()`), check whether one has already been migrated to the target parent version before troubleshooting from scratch — its pinned `slf4j-api`/`logback-classic`/`org.apache.sling.testing.paxexam`/`org.ops4j.pax.exam.version`/`org.apache.felix.framework` versions are a validated reference point.

## If testing.paxexam still needs a version bump

Some issues (e.g. an outdated `javax.servlet-api` bundled by an old `org.apache.sling.testing.paxexam`) do require bumping `org.apache.sling.testing.paxexam`. When you do:
- Bump only as far as needed, not necessarily to the latest release — check each candidate version's own POM for its required `org.ops4j.pax.exam.version` and prefer the smallest jump that resolves the issue, since larger jumps (e.g. into a version requiring pax-exam 4.14.0+) are what triggers the tinybundles/pax-exam-cm/probe-classloading cascade above.
- Always set `org.ops4j.pax.exam.version` (and any other `org.ops4j.pax.exam:*` artifacts pinned in the project) to match what that `testing.paxexam` version's own POM declares — check `~/.m2/repository/org/apache/sling/org.apache.sling.testing.paxexam/<version>/*.pom` for its `<org.ops4j.pax.exam.version>` property.

## See also

Once the bundle resolves and the quickstart starts, the same pax-exam IT can still fail for unrelated reasons on newer JDKs or slower CI agents — see [felix-http-jetty-jdk21.md](./felix-http-jetty-jdk21.md) and [pax-exam-timeouts.md](./pax-exam-timeouts.md).
