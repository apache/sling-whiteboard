[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-whiteboard/master)](https://builds.apache.org/job/Sling/job/sling-whiteboard/job/master) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Resource Graft

This module is part of the [Apache Sling](https://sling.apache.org) project.

## Grafting
> Grafting or graftage is a horticultural technique whereby tissues of plants are joined so as to continue their growth together. The upper part of the combined plant is called the scion while the lower part is called the rootstock.

_Source: https://en.wikipedia.org/wiki/Grafting_

## Description

This bundle contains utilities for grafting resource trees (scions) onto existing resources (rootstocks). It is intended to be used with Sling's ResourecDecorator API.

## Example

```java             
@Component
public class ExampleResourceDecorator implements ResourceDecorator {

    public Resource decorate(final Resource rootstock) {
        GraftedResource scion = GraftedResource.graftOnto(rootstock);
        scion.appendChild("foo") // see also appendChild() and insertBefore()
            .setProperty("bar", this is foo")
        .parent() // navigate back from "foo" to scion
        .setProperty("virtual-property", "I'm not persisted') 
        .removeProperty("existing") // hide existing property
        .removeChild("subtree"); // remove a complete subtree

        return scion;
    }
    ...    
}
```