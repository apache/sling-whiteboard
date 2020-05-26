Apache Sling GraphQL demo website
----

This is a work in progress demo of the Sling GraphQL Core.

## Status

At this point this module just provides a setup that starts the GraphQL Core module,
with the GraphQL Servlet active at `/graphql` and some sample content under
`/content/articles`.

Until we flesh this out, you'll need to add your own schemas and components
to run GraphQL queries.

## How to run this

* Build this module with `mvn clean install`
* Build the [GraphQL Core](https://github.com/apache/sling-org-apache-sling-graphql-core/) module
* Build the [Sling Kickstart](https://github.com/apache/sling-org-apache-sling-kickstart) module

Then start the demo Sling instance using

    rm -rf launcher/ conf/
    java -jar ${THE_CORRECT_PATH}/org.apache.sling.kickstart-0.0.3-SNAPSHOT.jar \
    -s src/main/resources/features/feature-sling12.json \
    -af src/main/resources/features/feature-graphql-example-website.json 

And open http://localhost:8080/content/graphql-website-demo.html

TODO: add GraphiQL instructions
