# Apache Sling Remote Content API - Sample App

To start this, first build the sibling modules then use

    mvn clean install exec:java

In this folder, and open http://localhost:8080 - which might require logging in
at http://localhost:8080/system/console first.

This should redirect you to a JSON content page, currently http://localhost:8080/content.rakam.json

The standard `MAVEN_OPTS` environment variable can be used to setup
debugging, as the above does not fork and starts the application with
the Maven JVM.

After that...well, the API is supposed to be _discoverable_ so you should find your way!

To activate debugging, use the standard `MAVEN_OPTS` - the Java code is started
directly in the Maven process.

The `/content/articles` subtree is where the most interesting content is, for now.

## Test Content

The test content uses `com.adobe.aem.guides:aem-guides-wknd.ui.content.sample` which is MIT
licensed. Minimal "fake" JCR nodetype definitions are used to allow this content to load, as
we don't really care about the details of these node types besides their names.