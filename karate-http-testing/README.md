HTTP Testing with Karate
========================

This is a series of experiments using Karate (https://github.com/intuit/karate) for testing Sling HTTP APIs.

It is used as the basis of my adaptTo 2018 talk, [Karate, the black belt of HTTP API testing?](https://adapt.to/2018/en/schedule/karate-the-black-belt-of-http-api-testing.html). The [slides are available](https://www.slideshare.net/bdelacretaz/karate-the-black-belt-of-http-api-testing) and a video recording should be available there soon.

To run these tests, start a Sling trunk instance on port 8080 (separately for now - should be automated) and run `mvn clean test`.

The Karate HTML reports (`target/surefire-reports/TEST-sling.*.html`) then display detailed HTTP interactions if tests fail, it's very useful for troubleshooting.

The test scenarios are found *.feature files under `src/test`, like [createContent.feature](src/test/java/sling/postservlet/createContent.feature) which should be self-explaining - that's the goal of all this.

See also https://github.com/bdelacretaz/karate-mini-mocks which is a minimal demonstration of the Karate "test doubles" which allow for mocking HTTP services with a very similar syntax than the feature tests.

Running the tests
-----------------
For now, the tests require a Sling instance to be started separately on port 8080. The simplest way to do that is to download the "Sling Starter Standalone" release 10 jar file from http://sling.apache.org/downloads.cgi and start it with `java -jar org.apache.sling.starter.*.jar` in an empty folder. To reset the state of that instance, if needed, stop it and delete the `sling` folder that's created when that jar file starts.

The karate "feature" test files are triggered by JUnit tests which have the `@RunWith(Karate.class)` annotation, like [SlingTest.java](src/test/java/sling/SlingTest.java), so they run as part of the standard Maven unit or integration tests.

To run test features which have a specific tag, use for example

    mvn clean test -Dcucumber.options="--tags @images"

And to run the Gatling performance tests, use

    mvn -o clean test-compile gatling:test && open $(find target/gatling -name index.html)

Karate notes
------------
The Karate UI is described at https://github.com/intuit/karate/wiki/Karate-UI
