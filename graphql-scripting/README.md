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

## Next Steps

  * This is just a rough prototype for now, in "the tests are green, who cares?" style. 
  * We'll probably need to invent a way to specify which `DataFetchers` to use in the schemas, for example decorating 
    them with information on how to select a suitable `DataFetchers` OSGi service based on service properties.
  
## Multiple GraphQL endpoint styles

This module enables the following GraphQL "styles"

  * The **traditional GraphQL endpoint** style, where the clients supply requests to a single URL. It is easy to define
    multiple such endpoints with different settings, which can be useful.
  * A **Resource-based GraphQL endpoints** style where every Sling Resource can be a GraphQL endpoint (using specific 
    request selectors and extensions) where queries are executed in the context of that Resource. This is an experimental
    idea at this point but isn't hard to support as it was part of the initial design of this module.
    
The second option supports both server-side "prepared GraphQL queries" which can be useful to aggregate content
for example, and the more traditional client-supplied queries.
  
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
