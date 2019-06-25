[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

# Apache Sling Feature Model diff tool

This tool aims to provide to Apache Sling users an easy-to-use tool which is able to detect differences between different released version of the same Apache Sling Feature Model.

## Simple APIs

Given two different versions of the same `org.apache.sling.feature.Feature`, all we need to do is comparing them

```java
import static org.apache.sling.feature.diff.FeatureDiff.compareFeatures;

import org.apache.sling.feature.Feature
import org.apache.sling.feature.diff.DiffRequest;
import org.apache.sling.feature.diff.DiffRequest;

...

Feature previous = // somehow obtained
Feature current = // somehow obtained

DiffRequest diffRequest = new DiffRequest()
                          .setPrevious(previous)
                          .setCurrent(current)
                          .setResultId("org.apache.sling:org.apache.sling.diff:1.0.0");

Feature featureDiff = compareFeatures(previous, current);
```

The resulting `featureDiff` is a new `Feature` instance which prototypes from `previous` and where necessary removals sections are populated and new elements may be added.

###Please note

The `FeatureDiff.compareFeatures(Feature, Feature)` rejects (aka throws an `IllegalArgumentException`) `Feature` inputs that:

 * are `null` (bien sûr);
 * refer exactly to the same `Feature`.

## Excluding sections

The `DiffRequest` data object can be configured in order to include/exclude one ore more Feature section(s), available are:

 * `bundles`
 * `configurations`
 * `extensions`
 * `framework-properties`

Users can simply add via the include/exclude methods the section(s) they are interested:

```java
DiffRequest diffRequest = new DiffRequest()
                          .setPrevious(previous)
                          .setCurrent(current)
                          .addIncludeComparator("bundles")
                          .addIncludeComparator("configurations")
                          .setResultId("org.apache.sling:org.apache.sling.diff:1.0.0");

Feature featureDiff = compareFeatures(previous, current);
```
