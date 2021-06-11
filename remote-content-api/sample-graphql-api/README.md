# Apache Sling Document-Oriented Content API - Sample GraphQL API

This sample provides a GraphQL query and command API that uses the sibling `document-aggregator` module
to output complete documents that aim to provide everything that's required to render a given Sling resource.

As I write this it doesn't use the SLING-10309 GraphQL pagination features yet, but it should.

To start this, build the sibling modules with `mvn clean install` in the parent folder
of this one and then, in this folder, run

    mvn clean install exec:java

Then open http://localhost:8080 - which might require logging in
at http://localhost:8080/system/console first.

This should redirect you to a test GraphQL endpoint, currently 
http://localhost:8080/content.N.json - which is meant to be used by a GraphQL client.

Note that with that _N_ selector, _every resource is a GraphQL endpoint_. This helps contextualize
requests, querying a `Folder` or `Document` without a _path_ parameter for example addresses
the current Resource. I'm thinking of implementing selector-driven prepared GraphQL requests
so that a GET request to _/content.N.folders.json_ for example would execute the prepared
_folders_ query against that Resource.

The standard `MAVEN_OPTS` environment variable can be used to setup
debugging, as the above does not fork and starts the application with
the Maven JVM.

## Test Content

The test content uses `com.adobe.aem.guides:aem-guides-wknd.ui.content.sample` which is MIT
licensed. Minimal "fake" JCR nodetype definitions are used to allow this content to load, as
we don't really care about the details of these node types besides their names.

## Example GraphQL queries

This prototype is evolving, some of these examples might be out of date. See the
[Schema for the API plane N](src/main/resources/schemas/default/N.GQLschema.jsp) for possible Queries and Mutations.

    # This one works at http://localhost:8080/content.N.json
    {
      folders(limit: 55, after: "L2NvbnRlbnQvYXJ0aWNsZXMvbXVzaWM=") {
        pageInfo {
          endCursor
        }
        edges {
          node {
            path
            header {
              resourceType
              title
            }
          }
        }
      }
    }

    # Works well at http://localhost:8080/content.N.json
    {
      folder {
        path
        header {
          parent
          resourceType
          resourceSuperType
          links {
            rel
            href
          }
        }
      }
      document(path:"/content/wknd") {
        path
        header {
          parent
          resourceType
        }
        body
      }
    }

    {
      folder(path: "/apps") {
        path
        header {
          parent
        }
      }
    }

    {
      document {
        path
          header {
          parent
          resourceType
          resourceSuperType
          title
          summary
          description
          links {
            href
            rel
          }
        }
        backstage {
          authoring
          publishing
          etc
        }
        body
      }
    }

    {
      documents(query: "//content/*/*") {
        edges {
          node {
            path
            header {
              parent
              resourceType
              resourceSuperType
            }
            body {
              source
              content
            }
            backstage {
              authoring {
                source
                content
              }
            }
          }
        }
      }
    }

    # we probably wouldn't use repoinit in this way - this
    # is just an example showing how to use a command pattern
    # via GraphQL mutations
    mutation {
    command(
      lang:"repoinit",
      input:"""
        # comments work here
        create path /open-for-all/ok
      """)
    {
      success
      output
      help
    }
    }

    # commands support free-form JSON data as input
    mutation {
      command(lang: "echo", input: 
        {structuredJSONdata: 
          {
            isSupported: true, 
            for: "things like this", 
            as: {json: "data"}
          }
        }
      	) {
        success
        output
        help
      }
    }

    mutation {
      command(lang: "echo", input: "Just a string") {
        success
        output
        help
      }
    }

    {
      documents(query: "//open-for-all/*") {
        edges {
          node {
            path
          }
        }
      }
    }

    {
      documents(lang: "sql2020", query: """
        select * from nt:unstructured as R
        where [sling:resourceType] = 'wknd/components/carousel'
        and isdescendantnode(R, '/content/wknd/us/en')
      """) {
        pageInfo {
          endCursor
        }
        edges {
          node {
            path
            body {
              content
            }
          }
        }
      }
    }