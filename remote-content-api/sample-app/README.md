# Apache Sling Remote Content API - Sample App

To start this, first build the sibling modules then use

    mvn clean install exec:java

And open http://localhost:8080 - which might require logging in
at http://localhost:8080/system/console first.

After that...well, the API is supposed to be _discoverable_ so you should find your way!

To activate debugging, use the standard `MAVEN_OPTS` - the Java code is started
directly in the Maven process.

The `/content/articles` subtree is where the most interesting content is, for now.
