# Apache Sling OSGi Metrics

This module is part of the [Apache Sling](https://sling.apache.org) project.

The OSGi metrics module defines a simple mechanism to gather OSGi-related metrics for application startup.

The module is split into two bundles:

- _collector_ - a zero-dependencies bundle that uses the OSGi APIs to gather various metrics
- _consumers_ - a single bundle that contains various consumers

The collector bundle dependes on a `org.apache.felix.systemready.SystemReady` service to detect
application startup. This service is provided by the [Apache Felix SystemReady](https://github.com/apache/felix-dev/blob/master/systemready/docs/README.md)
bundle.

## Usage

1. Add the `org.apache.sling/org.apache.sling.metrics.osgi.collector` bundle and ensure that
   it starts as early as possible
1. Add the `org.apache.felix/org.apache.felix.systemready` bundle and its dependencies, and
   configure it as desired
1. Add the `org.apache.sling/org.apache.sling.metrics.osgi.consumers` bundle. After the application
   startup is detected with reasonable confidence, the consumers will react. At least a logging
   statement should be printed in the log
