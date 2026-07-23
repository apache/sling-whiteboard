# pax-exam IT fails with ServiceLookupException on slower CI agents

Symptom — an IT using `@Inject`-annotated fields (e.g. `SlingRequestProcessor`, `ResourceResolverFactory`) fails intermittently, only on slower CI agents (observed on Windows), with:

```
org.ops4j.pax.swissbox.tracker.ServiceLookupException: gave up waiting for service org.apache.sling.engine.SlingRequestProcessor
```

Before assuming this is a timeout issue: check the full container log (`target/failsafe-reports/<TestClass>-output.txt`) for an actual bundle start failure — e.g. [felix-http-jetty-jdk21.md](./felix-http-jetty-jdk21.md). Increasing the timeout only helps if the required service genuinely just takes a long time to publish, not if it never comes up at all. If the failure happens at exactly ~10s (or whatever the current timeout is) every time regardless of platform, suspect a real startup failure first.

## The gotcha: `CoreOptions.systemTimeout(millis)` does NOT fix this

Do not use `org.ops4j.pax.exam.CoreOptions.systemTimeout(long)` in the pax-exam configuration — it has no effect on `@Inject`-field service-lookup timeouts. Those are read from a **system property** instead — verified by decompiling `org.ops4j.pax.exam.inject.internal.ServiceInjector` (in the `pax-exam-inject` artifact), whose `injectField` method reads it directly (parsed with `Integer.parseInt` and widened to `long`):

```java
long timeout = Integer.parseInt(System.getProperty("pax.exam.service.timeout", "10000"));
```

The default is 10 seconds (`org.ops4j.pax.swissbox.tracker.ServiceLookup.DEFAULT_TIMEOUT`), which can be too short for a full Sling quickstart (OSGi + Oak + scripting engines) to publish its services on a loaded or slower CI agent.

## Fix

Set the system property via `CoreOptions.systemProperty(...)` in the pax-exam configuration composite, not `systemTimeout(...)`:

```java
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

// in baseConfiguration():
systemProperty("pax.exam.service.timeout").value("60000"),
```

This is a JVM system property passed to the **forked container JVM** (where the OSGi framework and injected test run), not the outer Maven/surefire JVM — so it must be added as a pax-exam `Option`, not set via `-D` on the `mvn` command line.
