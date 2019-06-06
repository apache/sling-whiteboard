# Apache Sling URL connection agent

This module is part of the [Apache Sling](https://sling.apache.org) project.

This module provides a java agent that uses the instrumentation API to add timeouts to `connect` calls made via HTTP or HTTPs without setting read and connect timeouts. It is intended as an additional layer of control to use when running unstrusted client code that may make calls without explicitly setting timeouts.

## Launching

Build the project with `mvn clean package` and then run a simple connection test with 

    java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=<connect-timeout>,<read-timeout> -cp target/test-classes:target/it-dependencies/* org.apache.sling.uca.impl.Main <url> <client-type>
    
 The parameters are as follows:
 
 - `<connect-timeout>` - connection timeout in milliseconds
 - `<read-timeout>`- read timeout in milliseconds
 - `<url>` - the URL to access
 - `<client-type>` - the client type, either `JavaNet` for java.net.URL-based connections or `HC3` for commons-httpclient 3.x
 
 
 For a test that always fails, set one of the timeouts to 1. Both executions listed below will typically fail:
 
 ```
java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=1,1000 -cp target/test-classes:target/it-dependencies/* org.apache.sling.uca.impl.Main https://sling.apache.org JavaNet
java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=1000,1 -cp target/test-classes:target/it-dependencies/* org.apache.sling.uca.impl.Main https://sling.apache.org JavaNet
 ```
 
In contrast, the execution below should succeed:

```
java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=1000,1000 -cp target/test-classes:target/it-dependencies/* org.apache.sling.uca.impl.Main https://sling.apache.org JavaNet
```

## Tested platforms

* openjdk version "1.8.0_212"
* openjdk version "11.0.2" 2019-01-15
* commons-httpclient 3.1