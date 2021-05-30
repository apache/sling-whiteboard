# Apache Sling Document-Oriented Content API - Sample GraphQL API

This sample provides a GraphQL query and command API that uses the sibling `document-aggregator` module
to output complete documents that aim to provide everything that's required to render a given Sling resource.

As I write this it doesn't use the SLING-10309 GraphQL pagination features yet, but it should.

To start this, build the sibling modules with `mvn clean install` in the parent folder
of this one and then, in this folder, run

    mvn clean install exec:java

Then open http://localhost:8080 - which might require logging in
at http://localhost:8080/system/console first.

This should redirect you to the main GraphQL endpoint, currently 
http://localhost:8080/graphql.json - which is meant to be used by a GraphQL client.

The standard `MAVEN_OPTS` environment variable can be used to setup
debugging, as the above does not fork and starts the application with
the Maven JVM.

At this point, this module does not use snapshots from the sibling modules
in the Sling whiteboard - this bundle is the only snapshot declared in the
feature model file that thats this. 

## Test Content

The test content uses `com.adobe.aem.guides:aem-guides-wknd.ui.content.sample` which is MIT
licensed. Minimal "fake" JCR nodetype definitions are used to allow this content to load, as
we don't really care about the details of these node types besides their names.

## Example GraphQL queries

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

    {
      documents(query:"//content/*/*") {
        path
        body
      }
    }

    {
      documents(query:"//content//*[jcr:contains(.,'wknd')]") {
        path
      }
    }

    mutation {
      command(
        lang:"repoinit",
        script:"""
    	    # comments work here
          set properties on /open-for-all
            set title to "Look, I changed the title again!"
          end
    	    """) 
      {
        success
        output
      }
    }

    {
      document(path:"/open-for-all") {
        body
      }
    }

    {
      documents(
        lang:"sql2020",
        query:"""
          select * from nt:unstructured as R
          where [sling:resourceType] = 'wknd/components/carousel'
          and isdescendantnode(R, '/content/wknd/us/en')
        """) 
       {
        path
      }
    }