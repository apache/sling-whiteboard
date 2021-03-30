# Apache Sling Remote Content API

This is an _early days_ experiment to implement a document-oriented
Remote Content Access HTTP API for Sling content.

**STATUS** as of November 26th 2020: a simple way of converting Sling Resources to tree-like structures is implemented and used to generate a navigable HTTP API. The next step is to inject the [Sling Type System](https://cwiki.apache.org/confluence/display/SLING/Sling+Type+System%3A+motivation+and+requirements) ideas as a declarative way to customize the Resource conversions, define which Resource properties are useful for navigation, which ones can be ignored, how deep to recurse to build a document etc.

_Document-oriented_ means that the API exposes higher level objects than
Sling resources, typically documents which represent website pages and
similar content objects.

The API is meant to be fully discoverable, starting at the root should
be sufficient for you to discover all the content and how to use it.

It is meant to be friendly to both humans and machines, which is a challenge.
The well-defined scope of the API helps with that.

The API itself expose read-only content, but it includes links to 
a _command processor_ which will be implemented (separately, later)
to process content modifications and other commands. That processor
will use a CQRS-style Command Pattern,  where the read channel
represented by this API is distinct from the write channel (the 
Command Processor) and where things are not guaranteed to happen
synchronously.

## How to run this

See the sibling `sample-app` module.