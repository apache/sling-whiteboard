Apache Sling Content Parser API
====
This module is part of the [Apache Sling](https://sling.apache.org) project.

The Apache Sling Content Parser API provides support for parsing various files capable of abstracting a Sling resource tree. This API is a 
continuation of the one provided by the [Apache Sling JCR Content Parser](https://github.com/apache/sling-org-apache-sling-jcr-contentparser) bundle. Although very similar, there are some notable changes:

1. the API is now available in the `org.apache.sling.contentparser.api` package;
2. there is no replacement for the `org.apache.sling.jcr.contentparser.ContentParserFactory`; to obtain a `ContentParser`, given that 
they are exposed as OSGi services, one has to filter on the `ContentParser.SERVICE_PROPERTY_CONTENT_TYPE` service registration property,
to select the appropriate file format;
3. as a consequence of 2., the `ParserOptions` are now passed directly to the `ContentParser#parse` method.

Implementations of the API are made available from separate bundles:
1. JSON - [`org.apache.sling.contentparser.json`](https://github.com/apache/sling-whiteboard/tree/master/contentparser/org-apache-sling-contentparser-json)
2. XML - [`org.apache.sling.contentparser.xml`](https://github.com/apache/sling-whiteboard/tree/master/contentparser/org-apache-sling-contentparser-xml)
3. Jackrabbit Filevault XML ([Enhanced JCR 2.0 Document View](https://jackrabbit.apache.org/filevault/docview.html)) - [`org.apache.sling.contentparser.xml-jcr`](https://github.com/apache/sling-whiteboard/tree/master/contentparser/org-apache-sling-contentparser-xml-jcr) (the only module depending on the JCR / 
Jackrabbit APIs)

