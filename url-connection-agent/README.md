# Apache Sling URL connection agent

This module is part of the [Apache Sling](https://sling.apache.org) project.

This module provides a java agent that uses the instrumentation API to add timeouts to `connect` calls made via HTTP or HTTPs without setting read and connect timeouts.

## Launching

Build the project with `mvn clean package` and then run a simple connection test with 

    java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar -cp target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=<connect-timeout>,<read-timeout> org.apache.sling.uca.impl.Main <url>
    
 The parameters are as follows:
 
 - `<connect-timeout>` - connection timeout in milliseconds
 - `<read-timeout>`- read timeout in milliseconds
 - `<url>` - the URL to access
 
 For a test that always fails, set one of the timeouts to 1. Both executions listed below will typically fail:
 
 ```
java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=1,1000 -cp target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.apache.sling.uca.impl.Main https://sling.apache.org
java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=1000,1 -cp target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.apache.sling.uca.impl.Main https://sling.apache.org
 ```
 
In contrast, the execution below should succeed:

```
java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=1000,1000 -cp target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.apache.sling.uca.impl.Main https://sling.apache.org
```

## Tested platforms

* openjdk version "1.8.0_212"
* openjdk version "11.0.2" 2019-01-15