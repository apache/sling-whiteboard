[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

# Apache Sling Feature Model API Regions

This small set of APIs aims to provide to Apache Sling users an easy-to-use layer to manipulate
`api-regions` JSON extensions.

## Motivation

The `api-regions` extensions gained popularity via the [APIs Jar MOJO](https://github.com/apache/sling-slingfeature-maven-plugin/blob/master/src/main/java/org/apache/sling/feature/maven/mojos/ApisJarMojo.java) that, for each selected Apache Sling Feature Model file, generates multiple Java Archives, one for each declared section in the `api-regions` extension, containing declared APIs in each region.

During the time, business intelligence on top of `api-regions` extension will be required, i.e. analyzing the evolution of each region in each Apache Sling Feature Model file, to see what's removed and what's added (just to mention few operations), so having a single APIs set to manipulate `api-regions` will be helpful avoiding code redundancies across multiple applications.

## Quick how-to

`api-regions` extensions are generally obtained by parsing the JSON content in the `api-regions` declaration, i.e. given the following sample region:

```json
[
  {
    "name": "base",
    "exports": [
      "org.apache.felix.inventory",
      "org.apache.felix.metatype"
    ]
  },
  {
    "name": "extended",
    "exports": [
      "org.apache.felix.scr.component",
      "org.apache.felix.scr.info"
    ]
  }
]
```

then obtaining simple APIs will look like:

```java
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.apiregions.ApiRegion;
import org.apache.sling.feature.apiregions.ApiRegions;
import org.apache.sling.feature.apiregions.io.json.ApiRegionsJSONParser;

...

Feature feature = [somehow obtained]
// parseApiRegions method can be invoked also passing a org.apache.sling.feature.Extension instance
// or a String which represents the JSON structure of an api-regions extension.
ApiRegions regions = ApiRegionsJSONParser.parseApiRegions(feature);

for (ApiRegion region : regions) {
    System.out.println("-" + region.getName());

    for (String api : region) {
        System.out.println(" * " + api);
    }

    System.out.println("--------");
}
```

That code will output:

```
- base
 * org.apache.felix.inventory
 * org.apache.felix.metatype
--------
- extended
 * org.apache.felix.scr.component
 * org.apache.felix.scr.info
 * org.apache.felix.inventory
 * org.apache.felix.metatype
--------
```

## Note

Pleas take in account that each declared region in `api-regions` inherits declared APIs from the previous declared region.


## APIs to create regions

Users can also use APIs to manually declare their `api-regions` instances:

```java
omit imports for brevity;

...

ApiRegions apiRegions = new ApiRegions();
ApiRegion granpa = apiRegions.addNew("granpa");
granpa.add("org.apache.sling.feature.apiregions");

ApiRegion father = apiRegions.addNew("father");
father.add("org.apache.sling.feature.apiregions.io");

ApiRegion child = apiRegions.addNew("child");
child.add("org.apache.sling.feature.apiregions.io.json");

for (ApiRegion region : apiRegions) {
    System.out.println("-" + region.getName());

    for (String api : region) {
        System.out.println(" * " + api);
    }

    System.out.println("--------");
}
```

That code will output:

```
- granpa
 * org.apache.sling.feature.apiregions
--------
- father
 * org.apache.sling.feature.apiregions
 * org.apache.sling.feature.apiregions.io
--------
- child
 * org.apache.sling.feature.apiregions
 * org.apache.sling.feature.apiregions.io
 * org.apache.sling.feature.apiregions.io.json
--------
```

JSON serialization is available as well:

```java
import org.apache.sling.feature.apiregions.io.json.ApiRegionsJSONSerializer

...

ApiRegionsJSONSerializer.serializeApiRegions(ApiRegions, System.out);
```

that will output:

```json
[
  {
    "name":"granpa",
    "exports":[
      "org.apache.sling.feature.apiregions"
    ]
  },
  {
    "name":"father",
    "exports":[
      "org.apache.sling.feature.apiregions.io"
    ]
  },
  {
    "name":"child",
    "exports":[
      "org.apache.sling.feature.apiregions.io.json"
    ]
  }
]

```
