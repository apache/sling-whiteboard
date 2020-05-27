Apache Sling GraphQL demo website
----

This is a work in progress demo of the Sling GraphQL Core.

## Status

A first version is implemented with server-side GraphQL queries to demonstrate the simple
content aggregation that that provides, along with client-side Handlebars templates for
the demo.

http://localhost:8080/content/graphql-website-demo.html is the entry point, after starting
this as described below.

The GraphQL schemas and `DataFetchers` that have been implemented will be usable for more
traditional client-side GraphQL queries as well, once that's setup.

## How to run this

* Build this module with `mvn clean install`
* Build the [GraphQL Core](https://github.com/apache/sling-org-apache-sling-graphql-core/) module
* Build the [Sling Kickstart](https://github.com/apache/sling-org-apache-sling-kickstart) module

Then start the demo Sling instance using

    rm -rf launcher/ conf/
    java -jar ${THE_CORRECT_PATH}/org.apache.sling.kickstart-0.0.3-SNAPSHOT.jar \
    -s src/main/resources/features/feature-sling12.json \
    -af src/main/resources/features/feature-graphql-example-website.json 

And open the above mentioned start page.
