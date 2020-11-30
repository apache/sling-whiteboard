# Sling Type System and Remote Content API prototypes

This is a set of modules meant to demonstrate a Remote Content API for Sling,
based on the Type System idea described at
https://cwiki.apache.org/confluence/display/SLING/Sling+Type+System%3A+motivation+and+requirements

It's very much a work in progress for now.

See the [sample-app](./sample-app) folder for how to run the examples.

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