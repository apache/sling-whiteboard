# Apache Sling HTTP timeout enforcer

This module is part of the [Apache Sling](https://sling.apache.org) project.

This module provides a java agent that uses the [instrumentation API](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html) to add connect and read timeouts to `connect` made via HTTP or HTTPs. It only applies these timeouts if none were set explicitly.

The agent is intended as an additional layer of control to use when running untrusted client code that may make calls without explicitly setting timeouts. It is always recommended to set timeouts in client code, rather than relying on this agent.

It currently supports setting timeouts for HTTP connections done using:

* [java.net.URL](https://docs.oracle.com/javase/7/docs/api/java/net/URL.html) and/or [java.net.URLConnection](https://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html)
* [Apache Commons HttpClient 3.x](https://hc.apache.org/httpclient-3.x/)
* [Apache HttpComponents Client 4.x](https://hc.apache.org/httpcomponents-client-ga/)
* [OK Http](https://square.github.io/okhttp/)

## Validation

Build the project with `mvn clean package` and then run a simple connection test with 

    java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=<connect-timeout>,<read-timeout> -cp target/test-classes:target/it-dependencies/* org.apache.sling.uca.impl.HttpClientLauncher <url> <client-type>
    
 The parameters are as follows:
 
 - `<connect-timeout>` - connection timeout in milliseconds
 - `<read-timeout>`- read timeout in milliseconds
 - `<url>` - the URL to access
 - `<client-type>` - the client type, either `JavaNet` for java.net.URL-based connections ,`HC3` for Apache Commons HttpClient 3.x, `HC4` for Apache Commons HttpClient 4.x or `OkHttp` for OK HTTP.
 
 
 For a test that always fails, set one of the timeouts to 1. Both executions listed below will typically fail:
 
 ```
java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=1,1000 -cp target/test-classes:target/it-dependencies/* org.apache.sling.uca.impl.HttpClientLauncher https://sling.apache.org JavaNet
java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=1000,1 -cp target/test-classes:target/it-dependencies/* org.apache.sling.uca.impl.HttpClientLauncher https://sling.apache.org JavaNet
 ```
 
In contrast, the execution below should succeed:

```
java -javaagent:target/url-connection-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar=1000,1000 -cp target/test-classes:target/it-dependencies/* org.apache.sling.uca.impl.HttpClientLauncher https://sling.apache.org JavaNet
```

To use this in your own project you should 

## Tested platforms

* openjdk version "1.8.0_212"
* openjdk version "11.0.2" 2019-01-15
* commons-httpclient 3.1
* httpclient 4.5.4
* okhttp 3.14.2