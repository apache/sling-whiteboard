# Sling Feature Starter

This project is the Feature Model based version of the **sling-org-apache-sling-starter**
module and creates an executable JAR file for now.
It is also a test case for the Slingstart Feature Maven Plugin as it uses it
to launch a Launchpad Ready Rule and Smoke tests.

## Build

This plugin depends on the **Sling Start Feature Maven Plugin** (also in the Sling
Whiteboard) which is then used to run the IT tests:

1. Go to **sling-slingstart-feature-maven-plugin** module in Sling Whiteboard
2. Build with: `mvn clean install`
3. Go back to **sling-org-apache-sling-feature-starter**
4. Build and Launch it with: `mvn clean install`
5. Sling will come up and run the IT tests and then shut down. Sling can be
   kept running after the end of the IT tests by providing the property
   **block.sling.at.the.end** with the value **true**

## Usage

After the resulting jar file **org.apache.sling.feature.starter-<version>.jar**
can be executed with:
```
java -jar org.apache.sling.feature.starter-<version>.jar ...
```
