# Apache Sling Document-Oriented Content API - Sample GraphQL API

To start this, first build the sibling modules then use

    mvn clean install exec:java

In this folder, and open http://localhost:8080 - which might require logging in
at http://localhost:8080/system/console first.

This should redirect you to the main GraphQL endpoint, currently 
http://localhost:8080/graphql.json - which is meant to be used by a GraphQL client.

The standard `MAVEN_OPTS` environment variable can be used to setup
debugging, as the above does not fork and starts the application with
the Maven JVM.

## Test Content

The test content uses `com.adobe.aem.guides:aem-guides-wknd.ui.content.sample` which is MIT
licensed. Minimal "fake" JCR nodetype definitions are used to allow this content to load, as
we don't really care about the details of these node types besides their names.

## Example GraphQL queries

    { 
      document(path:"/content/articles/music/eloy-hahn-on-the-system-of-1080p-et-corrupti-aka-xml", selectors: "not, used, sofar") {
      	path
        selectors
        body
      }
    }
    
    {
      document(path:"/content/wknd/us/en/adventures/riverside-camping-australia", selectors: "not,used,yet") {
      	path
        selectors
        body
      }
    }