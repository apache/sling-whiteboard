# sling-graalvm
Experimenting with GraalVM to run a useful subset of Apache Sling modules, focusing on request processing, resource resolution and servlet resolution and execution for now.

See also [SLING-8556](https://issues.apache.org/jira/browse/SLING-8556).

To build and run the native version, setup GraalVM to get something like this:

    java -version
    openjdk version "1.8.0_212"
    OpenJDK Runtime Environment (build 1.8.0_212-20190523183630.graal2.jdk8u-src-tar-gz-b03)
    OpenJDK 64-Bit GraalVM CE 19.0.2 (build 25.212-b03-jvmci-19-b04, mixed mode)

Then install the `native-image` tool:

    gu install native-image

And build with 

    export GRAALVM_HOME=$JAVA_HOME
    mvn clean install -Pnative
    
And run with

    ./target/org.apache.sling.graalvm.experiments-1.0-SNAPSHOT-runner
    
At which point the server should work:

    $ curl -s http://localhost:8080/sling/chouc/route
    {
      "path": "/sling/chouc/route",
      "metadata": {
        "sling.resolutionPath": "/sling/chouc/route",
        "sling.resolutionPathInfo": "/sling/chouc/route"
      }
    }

To run as a Docker container see the [Dockerfile.native](./src/main/docker/Dockerfile.native) file.

The `time-to-first-request` utility is used to measure the time needed
by that container + our app to start up.

## TODO
Running in **quarkus:dev mode fails so far** ("no SCR metadata found"), even if 
using 

    mvn clean compile bnd:bnd-process quarkus:dev

The **native startup time** is about 5 seconds on my box, but just a few
milliseconds when runnning in a Docker image.
