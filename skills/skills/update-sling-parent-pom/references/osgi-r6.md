Replace the following Maven dependencies

- org.osgi:org.osgi.core → org.osgi:osgi.core
- org.osgi:org.osgi.compendium → org.osgi:osgi.cmpn

Remove any versions that are specified so they can be managed in the parent pom.

In case the Apache Felix framework is defined as a dependency - `org.apache.felix:org.apache.felix.framework` - make use the version is at least 4.6.1. This is needed for OSGi R6 support.
