graphql-scripting
----

This is an experiment to run GraphQL queries server-side in Sling.

It implements two Sling Scripting Engines, one for a `.gql` extension which
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