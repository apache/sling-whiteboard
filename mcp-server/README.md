# Apache Sling MCP Server

Experimental MCP Server implementation for Apache Sling.

## Usage

Build the project with Maven and start up the MCP server, based on the Apache Sling Starter:

```
$ mvn install feature-launcher:start feature-launcher:stop -Dfeature-launcher.waitForInput
```

Then build and deploy the sibling contributions package:

```
$ mvn -f ../mcp-server-contributions/ install sling:install 
```

Then open up your coding assistant tool and add an remote MCP server with location http://localhost:8080/mcp .
