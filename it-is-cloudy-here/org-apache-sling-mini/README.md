Apache Sling Mini
====

The Apache Sling Mini project provides a Sling Feature with a set of core bundles needed to start a Sling instance
capable of rendering dynamic websites. The instance intentionally doesn't provide a `ResourceProvider` implementation,
nor a `ScriptEngine` one, allowing an integrator to make these decisions.

The project also builds a Docker image (`org.apache.sling.mini`), which can be used as a base for a fully-fledged web 
application. Such an example is available in the [`org-apache-sling-mini-demo`](../org-apache-sling-mini-demo) folder.
