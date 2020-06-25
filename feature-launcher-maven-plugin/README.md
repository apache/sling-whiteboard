[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

# Apache Sling Feature Launcher Maven Plugin

This module is part of the [Apache Sling](https://sling.apache.org) project.

This plugin allows starting and stopping feature model applications without blocking the Maven
execution. It is intended mainly for usage with integration tests.

## Usage

Configure the plugin as follows:

```
<plugin>
    <groupId>org.apache.sling</groupId>
    <artifactId>feature-launcher-maven-plugin</artifactId>
    <configuration>
        <launches>
            <launch>
                <id>model</id>
                <feature>
                    <groupId>org.apache.sling</groupId>
                    <artifactId>org.apache.sling.starter</artifactId>
                    <version>12-SNAPSHOT</version>
                    <classifier>oak_tar</classifier>
                    <type>slingosgifeature</type>
                </feature>
                <launcherArguments>
                    <frameworkProperties>
                        <org.osgi.service.http.port>8080</org.osgi.service.http.port>
                    </frameworkProperties>
                </launcherArguments>
                <startTimeoutSeconds>180</startTimeoutSeconds>
            </launch>
        </launches>
        <toLaunch>
        </toLaunch>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>start</goal>
                <goal>stop</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

This will run the Sling Starter using the `oak_tar` aggregate, setting the HTTP port to 8080 and
waiting for up to 180 seconds for the application to start.

The `start` goal is bound by default to the `pre-integration` phase and the `stop` goal to the
`post-integration-test` one.

See the `src/it` folder for a complete example, including a configuration of the `maven-failsafe-plugin`.