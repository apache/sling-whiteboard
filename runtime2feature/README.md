[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

# Apache Sling OSGi runtime environment to Feature Model generator

## Running Feature

This is a simple OSGi service which is able to convert, given a `BundleContext` instance, a currently running OSGi container to an Apache Sling Feature Model definition.

APIs are really simple: it is necessary first to obtain the `RuntimeEnvironment2FeatureModel` instance from the OSGi Service Registry, then 

```
import org.apache.sling.feature.r2f.*;

@Reference
RuntimeEnvironment2FeatureModel generator;

...
Feature runtimeFeature = generator.getRunningFeature();
```

## Please Note

Currently version will include in the generated Feature Model `bundles` and `configurations` only, which are the only informations that can be extracted from a `BundleContext` instance.

## Launch Feature

The `RuntimeEnvironment2FeatureModel` OSGi service is also able to retrieve the (assembled) Feature used to launch the platform:

```
import org.apache.sling.feature.r2f.*;

@Reference
RuntimeEnvironment2FeatureModel generator;

...
Feature launchFeature = generator.getLaunchFeature();
```

## Upgrade Feature

The `RuntimeEnvironment2FeatureModel` OSGi service is also able to compute the upgrade Feature which prototypes from the Feature used to launch the platform and that targets the runtime Feature:

```
import org.apache.sling.feature.r2f.*;

@Reference
RuntimeEnvironment2FeatureModel generator;

...
Feature launchFeature = generator.getLaunch2RuntimeUpgradingFeature();
```

## The effective Runtime Feature

Finally, the `RuntimeEnvironment2FeatureModel` OSGi service is also able to compute the real runtime Feature which is assembled from the Feature used to launch the platform and that targets the runtime Feature:

```
import org.apache.sling.feature.r2f.*;

@Reference
RuntimeEnvironment2FeatureModel generator;

...
Feature launchFeature = generator.getLaunch2RuntimeUpgradingFeature();
```

