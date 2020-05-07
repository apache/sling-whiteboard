Apache Sling GraphQL Core
----

This module allows for running GraphQL queries in Sling, using dynamically built GraphQL schemas and
OSGi services for data fetchers (aka "resolvers") which provide the data.

To take advantage of Sling's flexibility, it allows for running GraphQL queries in three different modes,
using client or server-side queries and optionally being bound to the current Sling Resource.

Server-side queries are implemented as a Sling Script Engine.

The current version uses the [graphql-java](https://github.com/graphql-java/graphql-java) library, which
is exposed by the `org.apache.sling.graphql.api.graphqljava` interfaces. We might later remove this dependency
by creating a facade that abstracts these things, if needed.
 
## Supported GraphQL endpoint styles

This module enables the following GraphQL "styles"

  * The **traditional GraphQL endpoint** style, where the clients supply requests to a single URL. It is easy to define
    multiple such endpoints with different settings, which can be useful to provide different "views" of your content.
  * A **Resource-based GraphQL endpoints** style where every Sling Resource can be a GraphQL endpoint (using specific 
    request selectors and extensions) where queries are executed in the context of that Resource. This is an experimental
    idea at this point but it's built into the design so doesn't require more efforts to support. That style supports both
    server-side "**prepared GraphQL queries**" and the more traditional client-supplied queries.
    
The GraphQL requests can hit a Sling resource in all cases, there's no need for path-mounted servlets which are [not desirable](https://sling.apache.org/documentation/the-sling-engine/servlets.html#caveats-when-binding-servlets-by-path-1).
  
## Decorating the schemas with DataFetcher definitions

TODO rework this section and point to the corresponding tests.

With dynamic schemas, I think the mapping of types and/or fields to DataFetchers belongs in them.

We can use structured comments in the schemas for this so that they stay syntactically valid.

Here's an example where `## fetch:` comments are used for these definitions:

    type Query {

      # Use the 'test:pipe' Fetcher where "test" is a Fetcher namespace,
      # and pass "$" to it as a data selection expression which in this
      # case means "use the current Sling Resource".
      currentResource : SlingResource ## fetch:test/pipe $

    }

    type SlingResource {
      path: String
      resourceType: String

      # Use the "test:digest" DataFetcher with the "md5" fetcher option
      # and pass "$path" as its data selection expression
      pathMD5: String ## fetch:test/digest/md5 $.path

      # Similar with different fetcher options
      pathSHA512: String ## fetch:test/digest/sha512,armored(UTF-8) $.path
    }

A default `DataFetcher` is used for types and fields which have no `## fetch:` comment.

This is **not yet implemented** at commit 25cbb95d, there's just a basic parser for the above
fetch definitions.

See also [@stefangrimm's comment](https://github.com/apache/sling-whiteboard/commit/0c9db2d0e202eb74b605e65da7bfe01b4a8818f8#commitcomment-38639195) about using the graphql-java's `DataFetcher` API  to collect these definitions.

## How to test this in a Sling instance

See the `GraphQLScriptingTestSupport` class for which bundles need to be added to
a Sling Starter instance to support this bundle.

A simple way to install them in a Sling starter instance is:

    # Adapt this list based on the current version of the GraphQLScriptingTestSupport class!
    export B="graphql reactive antlr dataloader"

    mvn dependency:copy-dependencies
    export S=http://localhost:8080
    curl -u admin:admin -X DELETE $S/apps/gql
    curl -u admin:admin -X MKCOL $S/apps/gql
    curl -u admin:admin -X MKCOL $S/apps/gql/install
    curl -u admin:admin -X MKCOL $S/apps/gql/install/15
    for bundle in $B
    do
      path=$(ls target/dependency/*${bundle}* | head -1)
      filename=$(basename $path)
      curl -u admin:admin -T $path ${S}/apps/gql/install/15/${filename}
    done

And then install this bundle using for example

    mvn clean install sling:install

For some reason, as of April 14th the `org.apache.sling.installer.factory.packages` bundle
has to be stopped for this to work - didn't investigate so far.