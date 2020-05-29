Apache Sling GraphQL demo website
----

This is a work in progress demo of the [Sling GraphQL Core](https://github.com/apache/sling-org-apache-sling-graphql-core/).

It demonstrates both server-side GraphQL queries, used for content aggregation, and the 
more traditional client-side queries, using the same GraphQL schemas and data fetching
Java components for both variants.

Besides the page rendering code there's not much: GraphQL schema and query definitions and a few
Java classes used for aggregating or enhancing content and for content queries.

For now there's no pagination of query results, just arbitrary limits on the number
of results returned.

## Demo Website

A website with rich navigation is implemented with server-side GraphQL queries and client-side
Handlebars templates for HTML rendering.

http://localhost:8080/content/articles is the entry point, after starting
this as described below.

The articles and some navigation pages are rendered using server-side Handlebars templates,
which retrieve the aggregated JSON content of the current page by making an internal request
to the current path with a `.json` extension.

That aggregated JSON content is retrieved using server-side GraphQL queries so that a single
request provides all the page content and navigation.

Those `.json` URLs are also accessible from the outside if client-side rendering is preferred.

As a next step, to demonstrate "universal querying and rendering" the plan is to implement
a small single-page application for content browsing, using client-side GraphQL queries and
client-side Handlebars templates.

With this we'll get the best of both worlds: server-side queries and rendering for the article
pages (so that they make sense for Web search engines for example) and client-side queries and
rendering for the single-page applications that our website needs. Using the same languages
(GraphQL and Handlebars) in both cases, with a small amount of Java code to implement
the content querying and aggregation code.

## Client-side GraphQL queries

Client-side queries work using an external GraphiQL client (or any suitable client) that
talks to http://localhost:8080/graphql.json

For now we have a single "article with text" query to demonstrate the concept, for example:

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
only required activating the `GraphQLServlet` (using an OSGi config in the Feature Model
that starts this demo) and adding the below schema file. Everything else is shared between
the server-side and client-side query variants.

    # /apps/samples/servlet/GQLschema.jsp
    type Query {
      ## fetch:samples/articlesWithText
      article (withText : String) : [Article]
    }
    
    <%@include file="/apps/samples/common/GQLschema.jsp" %>

## How to run this

As this is early days, some assembly is required:

* Build the [GraphQL Core](https://github.com/apache/sling-org-apache-sling-graphql-core/) module
* Build the [Sling Kickstart](https://github.com/apache/sling-org-apache-sling-kickstart) module
* Build this module with `mvn clean install`

Then start the demo Sling instance using

    rm -rf launcher/ conf/
    java -jar ${THE_CORRECT_PATH}/org.apache.sling.kickstart-0.0.3-SNAPSHOT.jar \
    -s src/main/resources/features/feature-sling12.json \
    -af src/main/resources/features/feature-graphql-example-website.json 

And open the above mentioned start page.

## Under the hood

The following explanations apply to the article and navigation pages. The (upcoming) single-page apps
will use similar mechanisms client-side.

The scripts and source code mentioned below are found in the source code and initial content of this
demo module.

The GraphQL core retrieves a schema for the current Sling Resource by making a request with 
the `.GQLschema` extension. You can see the schemas by adding that extension to article and
navigation pages. They are generated using the standard Sling request processing mechanism, so
very flexible and resource-type specific if needed.

The server-side GraphQL queries are defined in `json.gql` scripts for each resource type. Here's
the current `article/json.gql` query as an example:

    { 
      navigation {
        sections {
          path
          name
        }
      }
      article 
      { 
        title
        tags
        seeAlso {
          path
          title
        }
        text
      }
    }

Based on that script's name, according to the usual Sling conventions it is used by the Sling GraphQL
ScriptEngine to execute the query and return a simple JSON document that provides everything needed
to render the page in one request. You can see those JSON documents by adding a `.json` extension to
article and navigation pages.

This JSON document includes navigation information (the content sections for now) and processed content
like the `seeAlso` links which are fleshed out by the `SeeAlsoDataFetcher` as the raw content doesn't 
provide enough information to build meaningful links. Such `DataFetcher` are then active for both
server-side and client-side GraphQL queries.

For this demo, the `.rawjson` extension provides the default Sling JSON rendering, for comparison or
troubleshooting purposes.