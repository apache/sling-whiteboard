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
  
## DataFetcher selection with Schema annotations

The GraphQL schemas used by this module can be enhanced with comments
that select specific `DataFetcher` to return the appropriate data.

A default `DataFetcher` is used for types and fields which have no such annotation.

Comments starting with `## fetch` specify data fetchers using the following syntax:

    ## fetch:<namespace>/<name>/<options> <source>

Where `<namespace>` selects a source (OSGi service) of `DataFetcher`, `<name>` selects
a specific fetcher from that source, `<options>` can optionally be used to adapt the 
behavior of the fetcher according to its own specification and `<source>` can optionally
be used to tell the fetcher which field or object to select in its input.
    
Here's an example of such an annotated schema.    

    type Query {
        ## fetch:test/echo
        currentResource : SlingResource
        ## fetch:test/static
        staticContent: Test
    }
    type SlingResource { 
        path: String
        resourceType: String

        ## fetch:test/digest/md5 path
        pathMD5: String
    
        ## fetch:test/digest/sha-256 path
        pathSHA256: String

        ## fetch:test/digest/md5 resourceType
        resourceTypeMD5: String
     }
    type Test { test: Boolean }

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