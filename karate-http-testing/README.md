HTTP Testing with Karate
========================

This is a series of experiments using Karate (https://github.com/intuit/karate) for testing Sling HTTP APIs.

It is used as the basis of my adaptTo 2018 talk, [Karate, the black belt of HTTP API testing?](https://adapt.to/2018/en/schedule/karate-the-black-belt-of-http-api-testing.html)

To run these tests, start a Sling trunk instance on port 8080 (separately for now - should be automated) and run `mvn clean test`.

The Karate HTML output (like `target/surefire-reports/TEST-sling.postservlet.createcontent.createContent.html`) then displays the detailed HTTP interactions, it's very useful for debugging the tests.

The test scenarios are found *.feature files under `src/test`, like [createContent.feature](src/test/java/sling/postservlet/createcontent/createContent.feature) which should be self-explaining - that's the goal of all this.

Karate notes
------------

Karate docs are at https://github.com/intuit/karate

A comment on Karate vs. Spock (by the author of Karate) is available at https://stackoverflow.com/questions/45352358/karate-vs-spock

The Karate UI is described at https://github.com/intuit/karate/wiki/Karate-UI
