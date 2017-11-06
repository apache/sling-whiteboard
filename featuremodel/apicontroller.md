# API Controller

If you're assembling a platform (in contrast to a final application) out of several features and provide this platform for customers to build their application on top if, an additional control of the API provided by the platform is needed. The bundles within the features provide all kinds of APIs but you might not want to expose all of these as extension points but rather want to use some of it internally within either a single feature or share within your features.

This is a proposal about how to add such additional metadata to the feature model. An API controller at runtime enforces the rules.

# Visibility of API

A feature exports some api, however there are different types of clients of the API:

* Bundles shipped as part of the platform
* Application bundles using the platform

We can generalize this by saying that API is either globally visible (to every client) or only visible to features within the same context. Usually this is referred to as a "region": The platform spawns its own region and a customer application has its own region, too. In theory there could be several customer applications running in the same framework on top of the platform, and each application has its own region.

Without any further information, API is globally visible by default. However, for platform features we want the opposite as we want to ensure that newly added API is not world-wide visible by default. Therefore we'll add an additional build time check (analyzer) that checks that each platform feature has an api controller configuration as below.

A feature can have an additional extension JSON named regions:

    "regions" : {
        "region" : "platform" // name of the region,
        "global-exports" : [
            "org.apache.sling.resource.api.*"
        ],
        "region-exports" : [
            "org.apache.sling.commons.scheduler"
        ]
    }

In the example above the feature declares:

* The name of the region it belongs to (platform)
* The list of packages it is exporting to everyone (Sling's resource API)
* The additional list of packages it is exporting to features within the same region (scheduler api)

Of course the above mentioned packages need to be exported by some bundle within the feature.

If the regions extension is missing, it is assumed that all packages are exported and that the feature runs in the global region.

If the region information is missing, the global region is used. If the packages list for global-exports or local-exports is missing, it is assumed to be all packages. If none should be exported, an empty array needs to be specified.

This model is intentionally kept simple and restricted to a feature being in only a single region. In theory we could think of use cases where a feature is shared across two (or more) regions, but not shared globally. However this gets pretty complicated easily and therefore we suggest to make such features global.

To support feature inheritance, a custom extension handler must be registered which will merge the extension - if the inherited one and the target feature use a different region, this is considered an error. If they have the same region, the packages are merged. Of course the inheriting feature can remove this extension before processing. In addition the extension handler must mark each bundle with the region, otherwise this relationship gets lost later on when the application is build.
