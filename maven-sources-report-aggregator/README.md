# Apache Sling Maven Repository Source Availability Reporter

This project offers a CLI interface to checking the availability of source artifacts on Maven Central.

## Usage

```
$ mvn clean verify
$ java -jar target/org.apache.sling.tooling.maven-sources-report-aggregator-*-jar-with-dependencies.jar org.apache*
```

The runnable jar takes a single argument, which is the groupId to match artifacts against. It is valid to add a trailing star,
which signals a prefix match.

The initial run may be slow, as the project downloads and imports the Maven Central index using the [Maven Indexer](https://maven.apache.org/maven-indexer/indexer-reader/index.html). 

After running, the report is placed in `out/results.csv`.