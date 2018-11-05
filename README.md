
Sling Capabilities - Oak Repository Descriptors
===============================================

This module is a prototype exposing a select set of Oak Repository Descriptors as [Sling Capabilities](https://github.com/apache/sling-org-apache-sling-capabilities).

Might make sense to merge it at some point with the [Jcr capabilities module](https://github.com/apache/sling-org-apache-sling-capabilities-jcr)

Usage
-----
The OakDescriptorSource will expose only those repository descriptor keys that are in a configured list (can use regex). So for exposing a descriptor key it must first be added to the OakDescriptorSource.keyWhitelist config.

The capabilities call will then return all whitelisted descriptor values, eg:

    {
      "org.apache.sling.capabilities": {
        "data": {
          "org.apache.sling.oak.descriptor": {
            "jcr.repository.name": "Apache Jackrabbit Oak",
            "jcr.repository.vendor": "The Apache Software Foundation"
          }
        }
      }
    }

