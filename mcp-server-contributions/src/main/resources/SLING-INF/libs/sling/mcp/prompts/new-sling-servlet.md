---
title: "Create new Sling Servlet"
description: "Creates a new Sling Servlet in the current project using annotations"
argument.resource-type:
  - title: Resource Type
  - required: true
  - description: The Sling resource type to bind this servlet to
---

# Create a new Sling Servlet

 Create a new Sling Servlet for resource type: `{resource-type}`
 
 Use the Sling-specific OSGi declarative services annotations - `@SlingServletResourceTypes` and `@Component` . 
 Configure by default with the GET method and the json extension.
 Provide a basic implementation of the doGet method that returns a JSON response with a message 'Hello from Sling Servlet at resource type {resource-type}'.
 