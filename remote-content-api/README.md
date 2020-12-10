# Sling Type System and Document-oriented Remote Content API prototypes

This is a set of modules meant to demonstrate a Remote Content API for Sling,
based on the Type System idea described at
https://cwiki.apache.org/confluence/display/SLING/Sling+Type+System%3A+motivation+and+requirements

It's very much a work in progress for now, see [SLING-9950](https://issues.apache.org/jira/browse/SLING-9950)

See the [sample-app](./sample-app) folder for how to run the examples.

## Use Cases

### Client-side rendering

For this use case, we want the API to be easy to consume both by the developers
who learn and discover it, and by client-side rendering code.

The client-side rendering code will usually not be provided by Sling for this use
case, but it could, using the mechanism described below for edge-side rendering.

![](src/docs/generated-diagrams/client-side-rendering.png)

### Edge-side rendering

For this use case we probably need to deliver both the content and the rendering templates
from Sling. We might use the current template resolution mechanism and include the URLs of
suggested templates, keyed by extension and selectors, with the content in order to simplify
the edge-side rendering code.

![](src/docs/generated-diagrams/edge-side-rendering.png)

## Initial requirements

The first goal: a hypertext-driven HTTP API that produces a set of navigable documents out of the sample 
content, using the type system.

By _Documents_ we mean that the API exposes higher level objects than
Sling resources, typically set of Sling Resources which represent website pages and
similar content objects.

The type system is used to decide which Resource fields to output in both the "navigation" and "content"
views of the API, and which Resource types are document roots.

Resource types for which the type system has no info are output with a generic rendering.

At this point we don't care how types are defined, they might be hardcoded.
