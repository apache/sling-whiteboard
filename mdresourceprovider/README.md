# Apache Sling Markdown Resource Provider

This module is part of the [Apache Sling](https://sling.apache.org) project.

It contains a work-in-progress markdown resource provider. The code is only lightly tested and meant as a proof-of-content. The only thing worse that the code is the documentation.

## Usage

Configure a `org.apache.sling.mdresource.impl.MarkdownResourceProvider`, e.g.

      org.apache.sling.mdresource.impl.MarkdownResourceProvider
        provider.file="../../whiteboard/mdresourceprovider/src/test/resources/"
        provider.root="/md-test"
        
Access http://localhost:8080/md-test.html.

### Meaning of special markdown constructs

As a genenal rule, the markdown is parsed and placed into the `jcr:description` property of the body. The following exceptions apply:

1. If a first-level heading is found, it is placed in the `jcr:title` property
1. If a [YAML front-matter](https://blog.github.com/2013-09-27-viewing-yaml-metadata-in-your-documents/) entry is found, its properties are parsed as strings and placed into properties of the resource.

These special rules are applied only for the first entries at the top of the file. As soon as a 'non-special' entry is found, special processing stops. This is an implementation limitation and can technically be removed. 
    

## TODO

- third-level recursive JSON access fails, e.g. `curl  http://localhost:8080/md-test.3.json`
- should we render the Markup in the ResourceProvider _or_ leave the raw markup and perform the rendering in a default script?
- allow mixing of plain file resources ( e.g. images, javascript, css ) to enable standalone service of image files
- testing
- documentation
- fix rat checks  