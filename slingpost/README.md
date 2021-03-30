[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Post Client

This module is part of the [Apache Sling](https://sling.apache.org) project.

Node JS Client for the [Sling POST Servlet](https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html).

## Importing

Install with:

    npm install @apachesling/slingpackager

and then import into your script:

    const SlingPost = require("@apachesling/slingpackager");

## Use

Construct a new instance with:

    const SlingPost = require("@apachesling/slingpost");

    const sp = new SlingPost();

The constructor takes an optional configuration object with the following options:

   * *url* {string} the url of the Apache Sling instance to connect to
   * *username* {string} the username to authenticate with Apache Sling
   * *password* {string} the password to authenticate with Apache Sling
   * *base* {string} the base directory to use when creating file paths

See the [JSDocs](docs/jsdoc.md) for full usage information.
