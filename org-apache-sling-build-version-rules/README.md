# Apache Sling Build Version Rules

This module is part of the [Apache Sling](https://sling.apache.org) project.

Use this for providing configuration to the versions-maven-plugin to filter out some of the known bad versions that exist in central from consideration in the versions:display-dependency-updates report.

## Usage
Build this project and then define the configuration in your own project pom.xml (or parent pom) with something like this:

```
  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>versions-maven-plugin</artifactId>
        <version>2.8.1</version>
        <configuration>
            <rulesUri>classpath:///org/apache/sling/build/maven-version-rules.xml</rulesUri>
        </configuration>
        <dependencies>
            <dependency>
                <groupId>org.apache.sling</groupId>
                <artifactId>org.apache.sling.build.version.rules</artifactId>
                <version>1.0-SNAPSHOT</version>
            </dependency>
        </dependencies>
      </plugin>
    </plugins>
  </build>
```
