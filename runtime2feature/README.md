[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

# Apache Sling OSGi runtime environment to Feature Model generator

This is a simple OSGi service which is able to convert, given a `BundleContext` instance, a currently running OSGi container to an Apache Sling Feature Model definition.

APIs are really simple: it is necessary first to obtain the `RuntimeEnvironment2FeatureModel` instance from the OSGi Service Registry, then 

```
import org.apache.sling.feature.r2f.*;

@Activate
BundleContext bundleContext;

@Reference
RuntimeEnvironment2FeatureModel generator;

...
ConversionRequest conversionRequest = new DefaultConversionRequest()
                                      .setResultId("org.apache.sling:org.apache.sling.r2e:jar:RUNTIME:1.0.0")
                                      .setBundleContext(bundleContext);
Feature runtimeFeature = generator.getRuntimeFeature(conversionRequest)
```

## Please Note

Currently version will include in the generated Feature Model `bundles` and `configurations` only, which are the only informations that can be extracted from a `BundleContext` instance.

## Launch Feature

The `RuntimeEnvironment2FeatureModel` OSGi service is also able to retrieve the (assembled) Feature used to launch the platform:

```
import org.apache.sling.feature.r2f.*;

@Activate
BundleContext bundleContext;

@Reference
RuntimeEnvironment2FeatureModel generator;

...
Feature launchFeature = generator.getLaunchFeature(bundleContext)
```
