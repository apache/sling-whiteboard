[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

Apache Sling GraphQL Schema Aggregator
----

This module ([SLING-10551](https://issues.apache.org/jira/browse/SLING-10551)) provides services to combine partial GraphQL
schema ("partials") supplied by _provider bundles_.

The partials are structured text files, supplied as OSGi bundle resources, that provide sections (like query,
mutation, types sections) that are aggregated to build a GraphQL Schema using the SDL (Schema 
Definition  Language) syntax.

A GraphQL schema must contain one `Query` statement and can contain a most one `Mutation` statement,
so partials cannot be assembled by just concatenating them. The schema assembler defines a simple
section-based syntax for the partials, so that they can be aggregated efficiently.

This module also provides a `SchemaAggregatorServlet` that generates schemas by aggregating partials, by
mapping request selectors to lists of partial names. The result can be used directly by the Sling GraphQL
Core module, which makes an internal Sling request to get the schema.

Partials can also depend on others by declaring the required dependencies by name, to make sure the
aggregated schemas are valid.

With this mechanism, an OSGi bundle can provide both a partial schema and the Sling data fetching and
processing services that go with it. This allows a GraphQL "API plane" (usually defined by a specific
instance of the Sling `GraphQLServlet`) to be built out of several OSGi bundles which each focus on a
specific set of queries, mutations and types.

## Provider bundles

To provide partials, a bundle sets a `Sling-GraphQL-Schema` header in its OSGi manifest, with a value that
points to a path under which partials are found in the bundle resources.

A partial is a text file with the structure described below. As usual, The Truth Is In The Tests, see
the [example partial in the test sources](./src/test/resources/partials/example.partial.txt) for a
reference that's guaranteed to be valid.

    # Example GraphQL partial schema
    # Any text before the first section is ignored.

    PARTIAL: Example GraphQL schema partial
    The contents of the PARTIAL section are ignored, only its
    description (the text follows the PARTIAL section name
    above) is used.

    PARTIAL is the only required section.

    REQUIRE: base.scalars, base.schema
    The description of the optional REQUIRE section is a
    comma-separated list of partials which are required for this
    one to be valid. The content of this section is ignored, only
    its description is used to build that list.

    PROLOGUE:
    The content of the optional PROLOGUE section is concatenated
    in the aggregated schema, before all the other sections.

    QUERY:
    The content of the optional QUERY sections of all partials
    is aggregated in a `type QUERY {...}` section in the output.

    MUTATION:
    Like for the QUERY section, the content of the optional
    MUTATION sections of all partials is aggregated in
    a `type MUTATION {...}` section in the output.

    TYPES:
    The content of the TYPES sections of all partials is
    aggregated in the output, after all the other sections.

## Partial names

The name of a partial, used in the selector mappings of the
`SchemaAggregatorServlet`, is defined by its filename in the
bundle resources, omitting the file extension. A partial
found under `/path-set-by-the-bundle-header/this.is.txt` in its bundle is named
`this.is` . Partial names must be unique system-wide, so it's
good to use some form of namespacing or agreed upon naming
convention for them.

## SchemaAggregatorServlet configuration
Here's a configuration example from the test code.

    // Configure the org.apache.sling.graphql.schema.aggregator.SchemaAggregatorServlet
    factoryConfiguration(AGGREGATOR_SERVLET_CONFIG_PID)
        .put("sling.servlet.resourceTypes", "sling/servlet/default")

        // The extension must be the one used by the GraphQLServlet to retrieve schemas
        // which by default is 'GQLschema'
        .put("sling.servlet.extensions", GQL_SCHEMA_EXT)

        // The GraphQLServlet uses an internal GET request for the schema
        .put("sling.servlet.methods", new String[] { "GET" })

        // Several selectors can be configured to setup API planes, each with their own GraphQL schema
        .put("sling.servlet.selectors", new String[] { "X", "Y" })

        // This mapping defines which partials to use to build the schema for each selector
        // The lists can use either the exact names of partials, or (Java flavored) regular expressions on
        // their names, identified by a starting and ending slash.
        .put("selectors.to.partials.mapping", new String[] { "X:firstA,secondB", "Y:secondA,firstB,/second.*/" })

## TODO / wishlist
The REQUIRES section of partial should be translated to OSGi capabilities, to be able to detect
missing requirements at system assembly time or using the
[Feature Model Analyser](https://github.com/apache/sling-org-apache-sling-feature-analyser).

We'll probably need a utility to aggregate schemas for automated tests, to allow test code
to include required schema partials.

Errors like invalid or missing partials are currently only logged, it would be useful to
have them cause louder errors, like schema aggregation failing with error messages when
things went wrong, and/or this module providing a Health Check service to detect problems.

Caching is probably not needed in this module, as the GraphQL Core caches compiled schemas.
