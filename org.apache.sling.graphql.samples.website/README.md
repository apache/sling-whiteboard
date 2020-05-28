Apache Sling GraphQL demo website
----

This is a work in progress demo of the Sling GraphQL Core.

It demonstrates both server-side GraphQL queries, used for content aggregation, and the 
more traditional client-side queries, using the same GraphQL schemas and data fetching
Java components for both variants.

Besides the page rendering code there's not much: GraphQL schema definitions and a few
Java classes used for aggregating or enhancing content and for content queries.

For now there's no pagination of query results, just arbitrary limits on the number
of results returned.

## Demo Website

A website with rich navigation is implemented with server-side GraphQL queries and client-side
Handlebars templates for HTML rendering.

http://localhost:8080/content/graphql-website-demo.html is the entry point, after starting
this as described below.

The rendering is based on JSON content that's aggregated server-side using GraphQL queries
to provide all the page content, navigation etc. in a single request.

This is just an initial prototype. As a next step I'd like to render the article pages using
server-side Handlebars templates and implement a few single-page applications for search
and browsing. While keeping the articles rendering server-side (so that they make sense
for Web search engines for example) and ideally using the same languages (GraphQL and
Handlebars) either server- or client-side.

## Client-side GraphQL queries

Client-side queries work using an external GraphiQL client (or any suitable client) that
talks to http://localhost:8080/graphql.json

For now only simple queries are supported, like:

    {
      article(withText: "jacobi") {
        path
        title
        tags
        seeAlso {
          title
          path
        }
      }
    }

Besides fixing the `DataFetcher`s to use the correct context Resource, setting this up
only required activating the `GraphQLServlet` (in the Feature Model that starts this demo)
and adding the below schema file. Everything else is shared between the server-side and 
client-side query variants.

    # /apps/samples/servlet/GQLschema.jsp
    type Query {
      ## fetch:samples/articlesWithText
      article (withText : String) : [Article]
    }
    
    <%@include file="/apps/samples/common/GQLschema.jsp" %>

## How to run this

* Build the [GraphQL Core](https://github.com/apache/sling-org-apache-sling-graphql-core/) module
* Build the [Sling Kickstart](https://github.com/apache/sling-org-apache-sling-kickstart) module
* Build this module with `mvn clean install`

Then start the demo Sling instance using

    rm -rf launcher/ conf/
    java -jar ${THE_CORRECT_PATH}/org.apache.sling.kickstart-0.0.3-SNAPSHOT.jar \
    -s src/main/resources/features/feature-sling12.json \
    -af src/main/resources/features/feature-graphql-example-website.json 

And open the above mentioned start page.
