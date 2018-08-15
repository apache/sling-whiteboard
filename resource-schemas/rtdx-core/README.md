Apache Sling RTD-X Extensions
=============================

As of January 19th, 2018 this is just a *very rough* proof of concept...

The RTD-X extensions generate generic hypermedia user interfaces to manage content,
based on resource models that define the various types of resources and how they can
be nested.

To test the demo models, install this bundle on Sling, create a resource with the `rtdx/root`
resource type and navigate to it with the `.rtdx.html` selector and extension.

The RTD-X module generates a basic HTML UI that allows you to navigate, create and edit resources
based on the models.

That interface is meant to be usable by machine clients as well, but that hasn't been tested so far.
Using standard hypermedia formats like Siren or HAL might be an option.

For now, the resource models are hardcoded in the `DemoModels` class. They should later be read from
structured text files provided as OSGi bundle resources or in the Sling content repository.