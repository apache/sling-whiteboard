Apache Sling GraphQL Scripting Engine
----

This is an experiment to run GraphQL queries server-side in Sling.

----

## Status at commit 61721472

The GraphQL engine is functional, a request to a Resource `/foo` mapped to a .gql script containing
`{ currentResource { path } }` returns `{currentResource={path=/foo}}`.
This demonstrates the server-side execution of GraphQL queries, in the context of the current Resource.

A (rough) `GraphQLServlet` allows for implementing either a traditional GraphQL single-path endpoint,
or turning any Sling Resource into a GraphQL endpoint. See the comments in that class.

----

## Next Steps
Here are my initial rough ideas for evolving this into a useful GraphQL core for Sling:

  * Use the Sling resolution mechanism to acquire the GraphQL Schemas, using maybe a `.gqlschema` request extension to get them. This provides full flexiblity to generate schemas from scripts, servlets, Sling Models or whatever's needed. We'll probably need to invent a way to specify which `DataFetchers` to use in the schemas.
  * Demonstrate how to create a "traditional" GraphQL endpoint by setting up a Sling Resource to define it, which might need a proxy servlet for ease of use. As those endpoints use the Sling script resolution mechanism under the hood, it's possible to define several such endpoints with specific characteristics, schemas etc. if needed. Also, backing those endpoints with Sling Resources allows for setting up access control of them, which we wouldn't get with a path-mounted servlet.
  
This (along with a few "details") would allow for this GraphQL core to be used in various modes:

  * As a traditional GraphQL endpoint, where the clients supply requests etc.
  * In a "Resource-based GraphQL endpoints" style where every Sling Resource can be a GraphQL endpoint (using specific request selectors and extensions), where queries are executed in the context of that Resource. For this we can either use server-side "prepared GraphQL queries" like in the initial prototype, or let the clients supply those queries to be closer to the traditional GraphQL way of doing things. 
  
The _Resource-based GraphQL endpoints_ mode is an experimental idea at this point but if we design for both modes from the beginning it's not hard to support - and I think server-side GraphQL queries can play an important role in some contexts.

## Initial Prototype

This module implements two Sling Scripting Engines, one for a `.gql` extension which
provides the actual GraphQL queries, and one for a `.gqls` extension which
provides GraphQL schema fragments.

Both script types use the usual Sling mapping mechanism where the Sling
resource type, HTTP request method, selectors and extension are taken
into account when selecting a script.

This allows GraphQL queries and schemas to be built dynamically based on
the current HTTP request and Sling Resource, without necessarily exposing
the actual GraphQL queries to the client.

Executing the queries in the context of the current request and Resource
might help kill the “GraphQL is the opposite of REST” myth and get the
best of both worlds. We’ll see ;-)

If we want to allow the client to supply those schemas and queries, which
might be useful for development, we might accept them as request parameters
if a "development mode" flag is set.

As an alternative to the `.gqls` scripts we might also use Sling Models to
provide GraphQL schema fragments, as those are already used in similar
use cases to define the “shape” of (mostly) JSON data returned to the client.

For now this is just an experiment meant to better understand how this can 
work and what it brings, we’ll refine the plan as we progress.

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
