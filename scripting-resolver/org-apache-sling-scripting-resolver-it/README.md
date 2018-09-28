Apache Sling Scripting Resolver (WIP, name not final)
====

## Integration Tests

To run the integration tests do:

```
mvn clean verify -Pit
```

## Example

To play around with a sling instance on localhost port 8080 (override with -Dhttp.port=<port>) that has the [examples](../examples) installed run:

```
mvn clean verify -Pexample
``` 
