# sling-graalvm
Experimenting with GraalVM to run Apache Sling modules

To build and run the native version, setup GraalVM to get something like this:

    java -version
    openjdk version "1.8.0_212"
    OpenJDK Runtime Environment (build 1.8.0_212-20190523183630.graal2.jdk8u-src-tar-gz-b03)
    OpenJDK 64-Bit GraalVM CE 19.0.2 (build 25.212-b03-jvmci-19-b04, mixed mode)

Then build with 

    export GRAALVM_HOME=$JAVA_HOME
    mvn clean install -Pnative
    
And run with

    ./target/org.apache.sling.graalvm.experiments-1.0-SNAPSHOT-runner
    
At which point the `/hello` path works:

    curl http://localhost:8080/hello
    Hello, at Mon Jul 01 17:38:00 CEST 2019

To run as a Docker container see `src/main/docker/Dockerfile.native`