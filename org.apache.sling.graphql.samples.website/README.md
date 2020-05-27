Apache Sling GraphQL demo website
----

This is a work in progress demo of the Sling GraphQL Core.

## Status

A website with rich navigation is implemented with server-side GraphQL queries and client-side
Handlebars templates for HTML rendering.

http://localhost:8080/content/graphql-website-demo.html is the entry point, after starting
this as described below.

The rendering is based on JSON content that's aggregated server-side using GraphQL queries
to provide all the page content, navigation etc. in a single request.</p>

This is just an initial prototype. I'd like to move closer to
[Progressive Web Apps](https://en.wikipedia.org/wiki/Progressive_web_application) techniques, 
generating a basic rendering server-side and enhancing it with CSS and JavaScript
client-side.

The GraphQL schemas and `DataFetchers` that have been implemented will be usable for more
traditional client-side GraphQL queries as well, once that's setup. As that side of things has
less unknowns, I'm focusing on the less usual server-side GraphQL concepts for now.

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
