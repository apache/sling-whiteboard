# Apache Sling JSON Store

TODO: Explain more, a content store using Sling that introduces that
stores content as JSON blobs validated by JSON schemas.

## Storage Model
* A _site_ is the root of a subtree of content that belongs together.
* Below the _site_ resource:
** The _schema_ subtree stores JSON schema keyed by resource type
** The _elements_ subtree stores validated reusable elements of content
** The _content_ subtree stores the actual validated content: pages etc.

## How to test this

Install this bundle and create a "JSON store root" node:

    curl -u admin:admin -F sling:resourceType=sling/jsonstore/root http://localhost:8080/content/sites

POST to that resource to create a test site:

    curl -u admin:admin -F path=example.com http://localhost:8080/content/sites

This creates the required structure under "sites/example.com" to store JSON schemas,
elements and content.