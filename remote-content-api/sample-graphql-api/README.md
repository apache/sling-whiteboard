# Apache Sling Document-Oriented Content API - Sample GraphQL API

This sample provides a GraphQL query and command API that uses the sibling `document-aggregator` module
to output complete documents that aim to provide everything that's required to render a given Sling resource.

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

## Next Steps
Here are a few ideas in no specific order.

Uppercase selectors in these notes refer to the _API planes_ concept, where the first uppercase selector addresses an "API plane" where
specific functionality is available, usually with a specific GraphQL Schema. The term "plane" is similar to a geometric plane, it's not
about flying metal tubes.

* Fix https://issues.apache.org/jira/browse/SLING-10485 to better support **various input types in Mutations**.
* Implement a small **content tree manipulation language** for the Command mutation. On a new API plane "C"? Can use a restricted variant of the repoinit language with just "create path" and "set ACL" along with delete and move operations. Provides similar functionality to the [Sling Post Servlet](https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html) in a more discoverable and especially strictly controlled way.
* Implement a **restrictive HTTP proxy to the Sling POST servlet** for updating content. On a new API plane "U"? For "updates". Might be restricted to the default "update content" operation and only if the target is either a Document or a Folder which can contain Documents. Other content manipulations are performed with the above content tree manipulation language, in order to restrict and better control them. POSTing content to that servlet can also happen with a GraphQL Mutation, we might support both variants but a direct POST is more efficient and might allow for streaming data which we don't have in the GraphQL core so far.
* Provide **hyperlinks to the various API planes** and/or explanations of how they work as part of the "links" field of Folders and Documents.
* Maybe rename the N plane to D as in "developement"?
* Add support for **selector-driven stored queries** so that `/content.P.folders.json` for example executes the 'folders' query that's been developed and stored from that developers plane. The P plane is for publishing, where HTTP GET requests return cacheable content based on stored queries.
* Add tests (using [Karate](https://github.com/intuit/karate)?) to this module to demonstrate the APIs if it looks like the module will graduate to a full-fledged Sling module. So far it's only tested interactively using the below example queries.

The overall idea is that accessing `/content.N.json` with a GraphQL client should be sufficient for a developer
to discover everything, via the commented GraphQL Schema provided there.

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
      documents(query: "//content/wknd/us/*/*") {
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
      command(lang: "echo", input: "Just a string, could also be an Integer, Float, Boolean, Array etc.") {
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

    {
      document(path: "/content/articles") {
        path
        backstage {
          authoring {
            content
          }
        }
      }
      folder {
        path
      }
      folders(path: "/content/wknd/") {
        edges {
          node {
            path
          }
        }
      }
      documents(query: "//content/wknd/us/*/*") {
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
