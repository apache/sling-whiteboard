# Resource Predicates

Provides a series of predefined predicates for Resources and Properties
for use with Collections and Streams

Examples

```java
new ResourceStream(resource)
	.stream(where(property("jcr:primaryType").is("page")))
	.filter(
      aChildResource("jcr:content")
          .has(property("sling:resourceType")
		    .isNot("sling/components/page/folder")))
    .collect(Collectors.toList());
```

```java
list.stream()
    .filter(
        property("jcr:content/sling:resourceType")
            .isNot("sling/components/page/folder"))
    .collect(Collectors.toList());
```

