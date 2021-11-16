# Apache Sling JSON Store

This module stores content as JSON blobs validated by JSON schemas.

## Storage Model
* A _site_ is the root of a subtree of content that belongs together.
* Below the _site_ resource:
  * The _schema_ subtree stores JSON schema keyed by resource type
  * The _elements_ subtree stores validated reusable elements of content
  * The _content_ subtree stores the actual validated content: pages etc.

## Integration tests

The [integration tests](./src/test/java/org/apache/sling/jsonstore/karate/)
(`*.feature` files) use [Karate](https://github.com/karatelabs/karate) and
demonstrate how to use the API.

## How to test this in a Sling instance

Install the following bundles, which you can get by running
` mvn dependency:copy-dependencies` in this folder:

    jackson-core-2.13.0.jar
    jackson-annotations-2.13.0.jar
    jackson-databind-2.13.0.jar
    json-schema-validator-1.0.63.jar

Install this bundle and verify that it is active.

http://localhost:8080/system/sling/openapi/openapi-browser.html provides
an OpenAPI browser. That UI provides examples that should help explore
the API use cases: store a schema, store a document, push the document
to a readonly branch and retrieve all those things.

## How to run the tests against an existing Sling instance

The integration tests start their own Sling instance, but you can
also run them against an existing one if desired, as follows:

    mvn verify -Dexternal.test.server.port=8080

or, to run just the Karate tests:

    mvn test -Dtest=KarateIT  -Dexternal.test.server.port=8080
