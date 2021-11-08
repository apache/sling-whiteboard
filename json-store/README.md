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

Create a "JSON store root" node:

    curl -u admin:admin -F sling:resourceType=sling/jsonstore/root http://localhost:8080/content/sites

POST to that resource to create a test site:

    curl -u admin:admin -F path=example.com http://localhost:8080/content/sites

This creates the required structure under "sites/example.com" to store JSON schemas,
elements and content.

POST a schema as follows:

    curl -u admin:admin -Fjson=@example-data/example-schema.json -FresourceType=example http://localhost:8080/content/sites/example.com/schema

And retrieve it as follows:

    curl -u admin:admin http://localhost:8080/content/sites/example.com/schema/example.tidy.5.json