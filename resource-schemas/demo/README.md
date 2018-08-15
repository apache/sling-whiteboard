Apache Sling Resource Schemas prototype - demo schemas
======================================================

To test the demo schemas, which allow for managing a very basic blog with folders and posts, install 
both the sibling core bundle and this one, create a resource with the `srs/demo/root`
resource type and navigate to it with the `.srs.html` selector and extension:

    $ curl -u admin:admin -F sling:resourceType=srs/demo/root http://localhost:8080/test
    $ curl -u admin:admin http://localhost:8080/test.srs.html
  
    <html><body><div class='srs-page'>
    <h1>Sling Resource Schemas: generated edit forms<br/>for /test</h1><hr/>
    ....

For now, the Resource Schemas are hardcoded in the `DemoSchemas` class. They should later be read from
structured text files provided as OSGi bundle resources or in the Sling content repository.
