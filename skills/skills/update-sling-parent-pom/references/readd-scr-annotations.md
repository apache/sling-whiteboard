# Add back SCR annotations to the pom.xml

In the build fails because the Felix SCR annotations are missing you can add them back to the pom.xml. They are not available by default in the parent pom but are part of the dependency management.

```xml
<dependency>
    <groupId>org.apache.felix</groupId>
    <artifactId>org.apache.felix.scr.annotations</artifactId>
    <scope>provided</scope>
</dependency>
```
