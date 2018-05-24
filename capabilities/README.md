Sling Capabilities Module
=========================

This is a work in progress for a service that allows for creating Capabilities endpoints
on a Sling instance: Resources that provide information on which services are available,
version levels etc.

Capabilities are computed by `Probes`, simple classes that can use Health Checks, access
external systems or do anything suitable to check what's available. Each `Probe` can return
multiple values as String key/value pairs.

To define a Capabilities endpoint, create a Sling Resource with 1..N properties having
names that end with the `_probe` suffix. The String value of each property is passed
to all available `ProbeBuilder` services, and the first one that returns a `Probe` is used.

As an example, with the default Probes found in this module, creating a Resource such as

    "caps": {
      "jcr:primaryType": "nt:unstructured",
      "foo_probe": "hc:The FOO cap:testing",
      "two_probe": "jvm:info",
      "3_probe": "hc:another HC:sometags",
      "sling:resourceType": "sling/capabilities"
    }
  
At `/tmp/caps` for example, and requesting `http://localhost:8080/tmp/caps.json` produces
the following output:

    {
      "org.apache.sling.capabilities": {
        "The FOO cap": {
          "TODO": "would run HCs with tags testing"
        },
        "JvmProbe": {
          "java.specification.version": "1.8",
          "java.vm.version": "25.171-b11",
          "java.vm.vendor": "Oracle Corporation"
        },
        "another HC": {
          "TODO": "would run HCs with tags sometags"
        }
      }
    }
    
This is not useful as is as the HC probes do not execute Health Checks yet - it's just meant
to demonstrate how Probes are created and used.    
