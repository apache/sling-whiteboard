[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling API Regions extension

This component contains extensions relating to the API Regions component.
The following extensions are registered via the ServiceLoader mechanism:

## `org.apache.sling.feature.builder.MergeHandler`
Merge handlers are called when features are merged during the aggregation process.

`APIRegionMergeHandler` - This handler knows how to merge API Regions extensions and adds the `org-feature` key to the `api-regions` sections to record what feature this section originally belonged.


## `org.apache.sling.feature.builder.PostProcessHandler`
PostProcessHandlers are called when a feature contains an `api-regions` section.

`BundleMappingHandler` - This handler creates a mapping file `idbsnver.properties` that maps the Artifact ID to a bundle symbolic name and version. A tilde `~` is used in the value of the map to separate BSN and version. 

`BundleArtifactFeatureHandler` - This handler creates 3 mapping files:
    `bundles.properties`: maps bundles to the original feature they were in. A bundle could be from more then one feature.
    `features.properties`: maps features to regions. A feature can be in more than one region.
    `regions.properties`: maps regions to packages. A region can expose more than one package.