# Apache Aries JAX-RS whiteboard experiments

This repository implements a minimal Sling application using the
[Aries JAX-RS Whiteboard](https://github.com/apache/aries-jax-rs-whiteboard)
modules to implement a HTTPs that can be documented with OpenAPI.

The [org.fipro.modifier.jaxrs](https://github.com/fipro78/access_osgi_services/tree/master/org.fipro.modifier.jaxrs)
example was useful in setting this up.

To start this, run

    mvn clean install exec:java

Then open http://localhost:8080 - which might require logging in
at http://localhost:8080/system/console first.

