Sling Capabilities Module
=========================

This servlet provided by this module allows for creating Capabilities HTTP endpoints
on a Sling instance: Resources that provide information on which services are available,
version levels etc.

For now, a single type of endpoint is provided: all Resources which have the
`sling/capabilities` resource type will return the same set of Capabilities, generated
by aggregating the output of all active `CapabilitiesSource` services.

This can be easily expanded to multiple sets of Capabilities if needed later on,
using service properties to group or tag the `CapabilitiesSource` services.

The tests provide simple `CapabilitiesSource` examples, that API is as follows:

    @ProviderType
    public interface CapabilitiesSource {

        /** @return the namespace to use to group our capabilities.
         *  That name must be unique in a given Sling instance.
         */
        String getNamespace();

        /** @return zero to N capabilities, each being represented by
         *      a key/value pair.
         * @throws Exception if the capabilities could not be computed.
         */
        Map<String, Object> getCapabilities() throws Exception;
    }

The `CapabilitiesServlet` produces output as in the example below, where two
`CapabilitiesSource` services are available:

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
