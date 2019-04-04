[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

# Apache Sling Feature Model diff tool

This tool aims to provide to Apache Sling users an easy-to-use tool which is able to detect differences between different released version of the same Apache Sling Feature Model.

## Simple APIs

Given two different versions of the same `org.apache.sling.feature.Feature`, all we need to do is comparing them

```java
import org.apache.sling.feature.Feature
import org.apache.sling.feature.diff.*;

...

Feature previous = // somehow obtained
Feature current = // somehow obtained
FeatureDiff featureDiff = FeatureDiff.compareFeatures(previous, current);

// check which section(s) are detected during the comparison

for (DiffSection diffSection : featureDiff.getSections()) {
    System.out.println(diffSection.getId());

    // Removed items from the current version, but present in the previous
    if (diffSection.hasRemoved()) {
        System.out.println(" - Removed");

        for (String value : diffSection.getRemoved()) {
            System.out.println("   * " + value);
        }
    }

    // Added items to the current version, but not present in the previous
    if (diffSection.hasAdded()) {
        System.out.println(" - Added");

        for (String value : diffSection.getAdded()) {
            System.out.println("   * " + value);
        }
    }

    // updated items from the previous to the current version
    if (diffSection.hasUpdatedItems() || diffSection.hasUpdates()) {
        System.out.println(" - Updated");

        for (UpdatedItem<?> updatedItem : diffSection.getUpdatedItems()) {
            System.out.println("   * " + updatedItem.getId() + " from " + updatedItem.getPrevious() + " to " + updatedItem.getCurrent());
        }

        for (DiffSection updatesDiffSection : diffSection.getUpdates()) {
            // there could be iteratively complex sub-sections
        }
    }
}
```

Please note that the `FeatureDiff.compareFeatures(Feature, Feature)` rejects (aka throws an `IllegalArgumentException`) `Feature` inputs that:

 * are `null` (bien sûr);
 * refer to completely unrelated, different `Feature`;
 * refer exactly to the same `Feature`.

## JSON representation

The `feature-diff` APIs contain a JSON serializer implementation, `org.apache.sling.feature.diff.io.json.FeatureDiffJSONSerializer`, which use is extremely simple:

```java
import org.apache.sling.feature.Feature
import org.apache.sling.feature.diff.FeatureDiff;
import org.apache.sling.feature.diff.io.json.FeatureDiffJSONSerializer;

...

Feature previous = // somehow obtained
Feature current = // somehow obtained
FeatureDiff featureDiff = FeatureDiff.compareFeatures(previous, current);

FeatureDiffJSONSerializer.serializeFeatureDiff(featureDiff, System.out);
```

output is quiet easy to interpret, i.e. from a unit test case:

```json
{
  "vendor":"The Apache Software Foundation",
  "vendorURL":"http://www.apache.org/",
  "generator":"Apache Sling Feature Diff tool",
  "generatorURL":"https://github.com/apache/sling-org-apache-sling-feature-diff",
  "generatedOn":"2019-04-04T11:08:51 +0200",
  "id":"org.apache.sling:org.apache.sling.feature.diff:1.0.0",
  "previousVersion":"0.9.0",
  "framework-properties":{
    "removed":[
      "sling.framework.install.incremental"
    ],
    "added":[
      "sling.framework.install.startlevel"
    ],
    "updated":{
      "env":{
        "previous":"staging",
        "current":"prod"
      }
    }
  },
  "bundles":{
    "removed":[
      "org.apache.sling:org.apache.sling.feature.diff:removed:1.0.0"
    ],
    "added":[
      "org.apache.sling:org.apache.sling.feature.diff:added:1.0.0"
    ],
    "updated":{
      "org.apache.sling:org.apache.sling.feature.diff:updated:2.0.0":{
        "updated":{
          "version":{
            "previous":"1.0.0",
            "current":"2.0.0"
          }
        }
      }
    }
  },
  "configurations":{
    "removed":[
      "org.apache.sling.feature.diff.config.removed"
    ],
    "added":[
      "org.apache.sling.feature.diff.config.added"
    ],
    "updated":{
      "org.apache.sling.feature.diff.config.updated":{
        "removed":[
          "it.will.appear.in.the.removed.section"
        ],
        "added":[
          "it.will.appear.in.the.added.section"
        ],
        "updated":{
          "it.will.appear.in.the.updated.section":{
            "previous":"[{/log}]",
            "current":"[{/log,/etc}]"
          }
        }
      }
    }
  }
}
```
