Apache Sling JCR API Wrappers (org.apache.sling.jcr.wrappers)
===

This module provides a set of classes that wrap the JCR API.

The wrappers just pass all calls through to the underlying implementation, they are meant to be extended to customize just the methods that need customizing.

Use Cases
----
This can be used to interecept calls to the JCR API in a way that's transparent to the clients.

Use cases include, but are not limited to:

 * Pre-loading or generating JCR content on-demand when specific API calls happen
 * Inserting delays or errors for testing
 * Converting content on the fly
 
So far we have only used this module for a limited implementation of content pre-loading as demonstrated by this module's tests.

Other usages will probably need expanding this module's test coverage which is incomplete for now.