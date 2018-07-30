Literate HTTP Testing with Karate
=================================

I'm experimenting with tools to write (very) readable tests for HTTP APIs, that could serve as reference documentation.

This is the Karate variant of this experiment, using https://github.com/intuit/karate

To run these tests, start a Sling trunk instance on port 8080 and run `mvn clean test`.

The Karate HTML output (like `target/surefire-reports/TEST-sling.postservlet.createcontent.createContent.html`) then displays the detailed HTTP interactions, it's very useful for debugging the tests.

The test scenarios are found *.feature files under `src/test`, like [createContent.feature](src/test/java/sling/postservlet/createcontent/createContent.feature) which should be self-explaining - that's the goal of all this.

Karate notes
------------

Karate docs are at https://github.com/intuit/karate

A comment on Karate vs. Spock (by the author of Karate) is available at https://stackoverflow.com/questions/45352358/karate-vs-spock
