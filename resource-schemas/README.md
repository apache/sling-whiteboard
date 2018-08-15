Apache Sling Resource Schemas prototype
=======================================

For now this is just a *rough* proof of concept...

The Sling Resource Schemas extensions generate generic hypermedia user interfaces to manage content,
based on resource schemas that define the various types of resources and how they can
be nested.

To test the demo schemas, which allow for managing a very basic blog with folders and posts, install 
this bundle on Sling, create a resource with the `srs/demo/root`
resource type and navigate to it with the `.srs.html` selector and extension:

    $ curl -u admin:admin -F sling:resourceType=srs/demo/root http://localhost:8080/test
    $ curl -u admin:admin http://localhost:8080/test.srs.html
  
    <html><body><div class='srs-page'>
    <h1>Sling Resource Schemas: generated edit forms<br/>for /test</h1><hr/>
    ....

This module generates a basic HTML UI that allows you to navigate, create and edit resources
based on the Resource Schemas.

That interface is meant to be usable by machine clients as well, but that hasn't been tested so far.
Using standard hypermedia formats like Siren or HAL might be an option.

For now, the Resource Schemas are hardcoded in the `DemoSchemas` class. They should later be read from
structured text files provided as OSGi bundle resources or in the Sling content repository.
