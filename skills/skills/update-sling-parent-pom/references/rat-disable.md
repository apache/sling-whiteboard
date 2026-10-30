The RAT profile was enabled by default with SLING-4511. If this causes failures right after upgrade, you can temporarily disable by running Maven with '-Drat.skip=true'.

If this succeeds add the property to the pom.xml file:

```xml
<properties>
    <rat.skip>true</rat.skip>
</properties>
```
