# Apache Sling MCP Server

Experimental MCP Server implementation for Apache Sling.

## Usage

Start up the MCP server, based on the Apache Sling Starter

```
$ mvn package feature-launcher:start feature-launcher:stop -Dfeature-launcher.waitForInput
```

Then open up your coding assistant tool and add an remote MCP server with location http://localhost:8080/mcp .
