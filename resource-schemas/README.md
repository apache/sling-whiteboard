Sling Resource Schemas prototype
================================

This is a (very rough for now) prototype using schemas to describe Sling resource types: their data model, 
actions that can be taken on specific resource types, default content, default access control etc.

The goal is to generate self-describing HTTP APIs as well as a basic (but progressively enhanceable) HTML UI to create content,
based on schemas that describe the content and behavior of resource types in detail.

The data part of the schemas aims to use JSON Schema principles, with extensions to define actions, default content, 
default access control and other non-data aspects.

However, or now schemas are just hardcoded in Java 
(in the [DemoSchemas.java](./demo/src/main/java/org/apache/sling/resourceschemas/demo/schemas/DemoSchemas.java) )
, as the goal is to demonstrate the overall behavior without caring much 
about such "details" as how the schemas are defined. at this point.
