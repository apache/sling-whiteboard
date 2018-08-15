Apache Sling Resource Schemas prototype - core
==============================================

For now this is just a *rough* proof of concept...

The Sling Resource Schemas extensions generate generic hypermedia user interfaces to manage content,
based on resource schemas that define the various types of resources and how they can
be nested.

This module generates a basic HTML UI that allows you to navigate, create and edit resources
based on the Resource Schemas.

That interface is meant to be usable by machine clients as well, but that hasn't been tested so far.
Using standard hypermedia formats like Siren or HAL might be an option.

The sibling "demo" bundle provides demo schemas, see its README for testing instructions.