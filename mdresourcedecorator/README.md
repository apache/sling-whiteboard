# Apache Sling Markdown Resource Decorator

This module is part of the [Apache Sling](https://sling.apache.org) project.

It contains a work-in-progress markdown resource decorator.

## Introduction

A resource decorator can decorate resources provided by any resource provider. For example, a decorator can enhance a resource with additional properties or change the resource type. The Markdown Resource Decorator can be used to parse markdown files provided by the resource decorated resource and then store the rendered html as well as metadata in the value map of the resource. 

## Installation

Install this bundle alongside with these two bundles:
- org.jsoup:jsoup:1.13.1
- com.vladsch.flexmark:flexmark-osgi:0.62.2

## Configuration

As the decorator is decorating resources from an already existing resource provider, you need to have a resource provider providing markdown files. You could use the file resource provider or store the files in the JCR repository.

Configure a `org.apache.sling.resource.MarkdownResourceDecorator`, e.g.

    "org.apache.sling.resource.MarkdownResourceDecorator" : {
        "decoration.paths" : "/content/files/**.md"
    }
        
With that all files ending in **.md** below **/content/files** will be decorated. An array of paths can be specified, each path can either be a pattern as above or just a prefix like **/myfiles** and then all resources in that tree will be decorated.

By default only files are decorated. This is ensured by checking the resource type of the resource. For allowing other resources, configure

        "decoration.types" : [ "my/type", "another/type"]
        
or allow any type with:

        "decoration.types" : "*"

The resource type of the decorated resource can be specified with

        "resource.type" : "sling/markdown/file"
          
The name of the property in the resource value map containing the rendered html can be set via

        "html.property" : "jcr:description"
         
If the title of the first heading should be extracted to a different property, specify

        "title.property" : "jcr:title"
            
or set this to a blank string to avoid special handling of the title.

If a [YAML front-matter](https://blog.github.com/2013-09-27-viewing-yaml-metadata-in-your-documents/) entry is found, its properties are parsed as strings or string arrays and placed into the value map of the resource.


    
