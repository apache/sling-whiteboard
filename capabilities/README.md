Sling Capabilities Module
=========================

This is a work in progress for a service that allows for creating Capabilities endpoints
on a Sling instance: Resources that provide information on which services are available,
version levels etc.

Capabilities are provided by `CapabilitiesSource` services, each of them having a unique
identifier used to namespace its capabilities, and providing a number of capabilities
as key/value pairs.

The `CapabilitiesServlet` is mounted on the `sling/capabilities` resource type, and produces
output as shown below, by aggregating the data provided by all `CapabilitiesSource` services.

    $ curl -s -u admin:admin http://localhost:8080/tmp/cap.json | jq .
    {
      "org.apache.sling.capabilities": {
        "org.apache.sling.capabilities.internal.OsgiFrameworkCapabilitiesSource": {
          "framework.bundle.symbolic.name": "org.apache.felix.framework",
          "framework.bundle.version": "5.6.10"
        },
        "org.apache.sling.capabilities.internal.JvmCapabilitiesSource": {
          "java.specification.version": "1.8",
          "java.vm.version": "25.171-b11",
          "java.vm.vendor": "Oracle Corporation"
        }
      }
    }
