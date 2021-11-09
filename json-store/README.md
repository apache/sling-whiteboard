# Apache Sling JSON Store

This module stores content as JSON blobs validated by JSON schemas.

## Storage Model
* A _site_ is the root of a subtree of content that belongs together.
* Below the _site_ resource:
  * The _schema_ subtree stores JSON schema keyed by resource type
  * The _elements_ subtree stores validated reusable elements of content
  * The _content_ subtree stores the actual validated content: pages etc.

## How to test this

Install the following bundles, which you can get by running
` mvn dependency:copy-dependencies` in this folder:

    jackson-core-2.13.0.jar
    jackson-annotations-2.13.0.jar
    jackson-databind-2.13.0.jar
    json-schema-validator-1.0.63.jar

Install this bundle and verify that it is active.

Store a JSON blob:

    curl -u admin:admin \
      -H "Content-Type: multipart/form-data" \
      -d @example-data/example-schema.json \
      http://localhost:8080/content/sites/example.com/schema/testing/onetwo

And retrieve it:

    curl http://localhost:8080/content/sites/example.com/schema/testing/onetwo
    