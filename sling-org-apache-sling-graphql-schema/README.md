[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

Apache Sling GraphQL Schema
----

This module ([SLING-10551](https://issues.apache.org/jira/browse/SLING-10551)) provides services to combine partial GraphQL
schema ("partials") supplied by _provider bundles_.

The partials are text files that use the GraphQL SDL (Schema Definition  Language) syntax and are
provided as OSGi bundle resources. We cannot name them "fragments" as that has a different meaning
in a GraphQL schema. They might include "front matter" a la Markdown for metadata.

A GraphQL schema must contain one `Query` statement and can contain a most one `Mutation` statement,
so partials cannot be assembled by just concatenating them. The schema assembler defines some simple 
rules for how to write the partials so that they can be aggregated efficiently.

This module also provides a `GraphQLSchemaServlet` that generates schemas by aggregating partials.
The result can be used directly by the Sling GraphQL Core module, which makes an internal Sling request
to get the schema. Multiple instances of that servlet can be configured, each with specific servlet
selection properties (selectors, extension etc.) along with a set of tags used to select which partials
are included in a specific schema.

With this mechanism, an OSGi bundle can provide both a partial schema and the Sling data fetching and
processing services that go with it. This allows a GraphQL "API plane" (usually defined by a specific
instance of the Sling `GraphQLServlet`) to be built out of several OSGi bundles which each focus on a
specific set of queries, mutations and types.

## Provider bundles

To provide partials, a bundle sets a `Sling-GraphQL-Schema` header in its OSGi manifest, with a value that
points to one or several paths where partials are found in the bundle resources.

A partial is a text file with a `.graphql.partial.txt` extension that has the following structure:

    # The "org.apache.sling.*" comments in the partial are used by the schema assembler, which might
    # impose constraints on their presence and values.
    #
    # org.apache.sling.partial.name: Folders and commands
    # org.apache.sling.tags: folder, command, development, authoring
    #
    # The aggregated schema will include additional org.apache.sling.* comments to provide information
    # on the aggregation process and help troubleshoot it.

    # If a partial contains a Query and/or Mutation statement, the schema assembler uses their
    # indentation to parse them without having to consider the full SDL syntax.
    #
    # These Query and Mutation keywords, along with their closing bracket, MUST NOT be indented,
    # and everything inside them MUST be indented by at least one whitespace character.

    type Query {
        """ 
        Query a single Folder.
        If not specified, the path defaults to the Resource which receives the query request.
        """
        folder(path: String) : Folder @fetcher(name:"samples/folder")
    }

    type Mutation {
        """ 
        'lang' is the command language
        'script' is the script to execute, in the language indicated by 'lang'
        """  
        command(lang: String, input: Object) : CommandResult @fetcher(name:"samples/command")
    }

    # There are no constraints on the rest of the schema, which the assembler simply concatenates
    # after the Query and Mutation sections
    type Folder {
      path : ID!
    }

## Implementation notes

TODO remove those once the module is implemented.

We can use an [OSGi Extender Pattern](https://enroute.osgi.org/FAQ/400-patterns.html) to handle the
provider bundles, similar to what we do for 
[initial content loading](https://sling.apache.org/documentation/bundles/content-loading-jcr-contentloader.html)
.

The assembler bundle can use a
[BundleTracker](https://docs.osgi.org/javadoc/r4v42/org/osgi/util/tracker/BundleTracker.html)
to detect the provider bundles based on their manifest header.

It can use logic similar to the jcr content loader module to load the partial schema text files:

* https://github.com/apache/sling-org-apache-sling-jcr-contentloader/blob/master/src/main/java/org/apache/sling/jcr/contentloader/PathEntry.java
* Bundle.getEntryPaths to enumerate file resources in the bundle

On our dev list, Radu suggests creating an OSGi a capability in o.a.s.graphql.schema that the bundles which provide schema extensions require, in order to create the wiring in between the bundles. This allows a limited number of bundles to trigger the BundleTracker, creating a weak contract.

