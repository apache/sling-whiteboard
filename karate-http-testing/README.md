HTTP Testing with Karate
========================

This is a series of experiments using Karate (https://github.com/intuit/karate) for testing Sling HTTP APIs.

It is used as the basis of my adaptTo 2018 talk, [Karate, the black belt of HTTP API testing?](https://adapt.to/2018/en/schedule/karate-the-black-belt-of-http-api-testing.html)

To run these tests, start a Sling trunk instance on port 8080 (separately for now - should be automated) and run `mvn clean test`.

The Karate HTML reports (`target/surefire-reports/TEST-sling.*.html`) then display detailed HTTP interactions if tests fail, it's very useful for troubleshooting.

The test scenarios are found *.feature files under `src/test`, like [createContent.feature](src/test/java/sling/postservlet/createContent.feature) which should be self-explaining - that's the goal of all this.

See also https://github.com/bdelacretaz/karate-mini-mocks which is a minimal demonstration of the Karate "test doubles" which allow for mocking HTTP services with a very similar syntax than the feature tests.

Running the tests
-----------------
The karate "feature" test files are triggered by JUnit tests which have the `@RunWith(Karate.class)` annotation, like [SlingTest.java](src/test/java/sling/SlingTest.java), so they run as part of the standard Maven unit or integration tests.

To run test features which have a specific tag, use for example

    mvn clean test -Dcucumber.options="--tags @images"

And to run the Gatling performance tests, use

    mvn -o clean test-compile gatling:test && open $(find target/gatling -name index.html)

Karate notes
------------
The Karate UI is described at https://github.com/intuit/karate/wiki/Karate-UI