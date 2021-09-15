# Apache Aries JAX-RS whiteboard experiments

This repository implements a minimal Sling application using the
[Aries JAX-RS Whiteboard](https://github.com/apache/aries-jax-rs-whiteboard)
modules to implement HTTP APIs that can be documented with [OpenAPI](https://www.openapis.org/).

The [org.fipro.modifier.jaxrs](https://github.com/fipro78/access_osgi_services/tree/master/org.fipro.modifier.jaxrs)
example was useful in setting this up.

To start this, run

    mvn clean install exec:java

Then open http://localhost:8080 - which might require logging in
at http://localhost:8080/system/console first.

http://localhost:8080/api/jaxrs/test/testing.this (for example) should
then address the JAX-RS [`TestService`](./src/main/java/org/apache/sling/jaxrs/TestService.java) resource.

The corresponding OpenAPI JSON document is available at http://localhost:8080/api/openapi/openapi.json
with an API browser at http://localhost:8080/api/openapi/openapi-browser.html

All other URLs outside of `/api/jaxrs` and `/api/openapi`, like http://localhost:8080/api/sling.json 
are served by Sling as usual.

A POST can increment the test counter, such as:

    curl -XPOST http://localhost:8080/api/jaxrs/test/increment/42

## TODO

* The JAX-RS servlet should use Sling authentication, mounting it on
a resource type might be interesting. The servlet that serves JAX-RS requests is currently registered by the [Aries Whiteboard class](https://github.com/apache/aries-jax-rs-whiteboard/blob/f6a23cd19e567c959ac71893f4f6013715680299/jax-rs.whiteboard/src/main/java/org/apache/aries/jax/rs/whiteboard/internal/Whiteboard.java#L1303)
* Provide an [OpenAPI](https://www.openapis.org/) document describing the API.
