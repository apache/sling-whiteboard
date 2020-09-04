# Apache Sling Markdown Resource Provider

This module is part of the [Apache Sling](https://sling.apache.org) project.

This module contains [Health Checks](https://github.com/apache/felix-dev/tree/master/healthcheck) that use
the Sling Resource API.

## Usage

```
"org.apache.sling.hc.resource.impl.ResourceHealthCheck~slingshot": {
    "hc.tags":[
        "systemalive"
    ],
    "resources": [
        "/content/slingshot",
        "/libs/slingshot/Home/html.jsp"
    ]
}
```
