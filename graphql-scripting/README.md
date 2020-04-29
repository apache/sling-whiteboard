Apache Sling GraphQL Scripting Engine
----

This is an experimental module for running GraphQL queries in Sling.

----

## Status at commit 13847b2b

The GraphQL engine is functional, a request to a Resource `/foo` mapped to a .gql script containing
`{ currentResource { path } }` returns `{currentResource={path=/foo}}`.
This demonstrates the server-side execution of GraphQL queries, in the context of the current Resource.

A (rough) `GraphQLServlet` allows for implementing either a traditional GraphQL single-path endpoint,
or turning any Sling Resource into a GraphQL endpoint. See the comments in that class.

The GraphQL schema is provided by a sub-request using the `.GQLschema` extension - this allows us to
implement any suitable dynamic mechanism to provide it.

----

## Decorating the schemas with DataFetcher definitions

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


## Multiple GraphQL endpoint styles

This module enables the following GraphQL "styles"

  * The **traditional GraphQL endpoint** style, where the clients supply requests to a single URL. It is easy to define
    multiple such endpoints with different settings, which can be useful to provide different "views" of your content.
  * A **Resource-based GraphQL endpoints** style where every Sling Resource can be a GraphQL endpoint (using specific 
    request selectors and extensions) where queries are executed in the context of that Resource. This is an experimental
    idea at this point but it's built into the design so doesn't require more efforts to support. That style supports both
    server-side "**prepared GraphQL queries**" and the more traditional client-supplied queries.
  
## How to test this in a Sling instance

See the `GraphQLScriptingTestSupport.graphQLJava()` method for which bundles
are required for this bundle to start.

A simple way to install them in a Sling starter instance (running under JDK11
or later) is:

    mvn dependency:copy-dependencies
    export S=http://localhost:8080
    curl -u admin:admin -X DELETE $S/apps/gql
    curl -u admin:admin -X MKCOL $S/apps/gql
    curl -u admin:admin -X MKCOL $S/apps/gql/install
    curl -u admin:admin -X MKCOL $S/apps/gql/install/15
    export B="graphql reactive antlr dataloader"
    for bundle in $B
    do
      path=$(ls target/dependency/*${bundle}* | head -1)
      filename=$(basename $path)
      curl -u admin:admin -T $path ${S}/apps/gql/install/15/${filename}
    done

For some reason, as of April 14th the `org.apache.sling.installer.factory.packages` bundle
has to be stopped for this to work - didn't investigate so far.

And then install this bundle using for example

    mvn clean install sling:install
