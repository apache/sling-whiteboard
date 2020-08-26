Apache Sling Remote Document-oriented API (dokapi)
----

This is a _very early days_ experiment to implement a document-oriented
HTTP API for Sling content.

_document-oriented_ means that the API exposes higher level objects than
Sling resources, typically documents which represent website pages and
similar content objects.

The API is meant to be fully discoverable, starting at the root should
be sufficient for you to discover all the content and how to use it.

The API itself is read-only but includes links to a _command processor_
which will be implemented (later) to process content modifications and
other commands. That processor will use a CQRS-style Command Pattern, 
where the read channel represented by this API is distinct from the
write channel (the Command Processor) and where things are not
guaranteed to happen synchronously.

How to run this
===

Build and start this with

    mvn clean install exec:java

And open the _dokapi_ root page at

    http://localhost:8080

After that...well, the API is supposed to be _discoverable_ so you should find your way!

The `/content/articles` subtree is where the most interesting content is, for now.