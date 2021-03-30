# Sling Type System

This is based on the ideas at
https://cwiki.apache.org/confluence/display/SLING/Sling+Type+System%3A+motivation+and+requirements

## Tentative Initial Requirements

The Sling Type System supplies Sling Type Definitions.

A Sling Type is usually specified by the `sling:resourceType` value of a Sling Resource.

The Sling Type defines the structure of the Resource in terms of Fields.

Each Field has at least a name and data type (String, Number, Boolean etc) and can have
additional attributes for validation, linking with other Resources, textual descriptions and
other application-specific attributes with names that are not initially known - but should be
namespaced to avoid name collisions.

For now, the Sling Type System is meant to be used to generate a implement an hypertext-driven
HTTP Remote Content API. Next is the generation of GraphQL Schemas for query-based use cases, and
there are many places in Sling where a Type System can be useful.