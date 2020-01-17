# Sling Start Feature Maven Plugin

This Maven Plugin is the Feature Model based version of the Slingstart
Maven Plugin. It does not depend on its predecessor to keep the Provisioning Model
and Feature Model code bases separate.

## Build

This plugin is built like usual with:
```
mvn clean install
```

## Usage

The plugin can be used (see **sling-org-apache-sling-feature-starter**
module in Sling Whiteboard) like this:
```
<plugin>
    <groupId>org.apache.sling</groupId>
    <artifactId>slingstart-feature-maven-plugin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <extensions>true</extensions>
    <executions>
        <execution>
            <id>start-container</id>
            <goals>
                <goal>start</goal>
                <goal>stop</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <launchpadJar>${project.build.directory}/${project.artifactId}-${project.version}.jar</launchpadJar>
        <parallelExecution>false</parallelExecution>
        <servers>
            <server>
                <port>${http.port}</port>
                <controlPort>${sling.control.port}</controlPort>
                <debug>true</debug>
                <stdOutFile>launchpad.out</stdOutFile>
            </server>
        </servers>
    </configuration>
</plugin>
```

## Notes

For now this Plugin only supports the starting and stopping of a Sling
instance for example to run IT tests.
