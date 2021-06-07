[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

# Apache Sling Sitemap

The Apache Sling Sitemap module is an extension for Apache Sling that helps to serve XML Sitemaps to Search Engines. It
was designed to cover various uses cases from small sites serving sitemaps on-demand, large sites generating them in the
background to even sites that collect 3rd party data to include dynamically rendered pages.

## Highlights

* A simple, builder-like API to create Sitemaps, that hides all the XML specifics from the implementation
* Supports on-demand and background generation w/ continuation after job interruption
* Support for nested sitemaps, that are automatically collected into a sitemap indexes

## Open Topics

* Housekeeping of SitemapStorage (used for background generation), esp. when sitemap roots change
* More general approach for creating absolute urls.
* Implement Google specific sitemap extensions (image/video/news)

## Getting Started

To get started, at least one `SitemapGenerator` must be implemented. The abstract `ResourcceTreeSitemapGenator` may be a
good starting point for any generator walking down the resource tree.

Next the `SitemapServlet` must be registered for the appropriate resource type(s) that match the content.

Last but not least, either configure the `SitemapService` implementation to serve your sitemaps on-demand, or configure
a `SitemapScheduler` to generate them in the background.

## Implementation Details

### Content Model

In order to serve a sitemap, a resource must be marked as sitemap root resource. This is done by adding
a `sitemapRoot = true` property either to the resource, or it's `jcr:content` child.

When multiple resources in a resource tree are marked as as sitemap roots, the on closest to the repository root is
considered to top level sitemap root and serves a sitemap-index additionally to the sitemap.

```
/content/site/ch/
 + de-ch/
  - sitemapRoot = true
  + faqs/
  + news/
  + products/
   - sitemapRoot = true
 + it-ch/
  - sitemapRoot = true 
  ...
```

In the example above the paths `de-ch/` and `fr-ch/` are both top level sitemap roots and `de-ch/products/` is a nested
sitemap root. The sitemaps will be served as following, assuming the appropriate mappings being in place:

```
https://site.ch/de-ch.sitemap-index.xml
https://site.ch/de-ch.sitemap.xml
https://site.ch/de-ch.sitemap.products-sitemap.xml
https://site.ch/fr-ch.sitemap.xml
```

### Sitemap Generation

The module does not ship a specific `SitemapGenerator` implementation. Products/Projects using the Apache Sling Sitemap
module must implement an appropriate `SitemapGenerator` that fits their content model. An abstract
`ResourcceTreeSitemapGenator` implementation is available to cover the most common use cases.

Each `SitemapGenerator` may produce multiple sitemaps for a given sitemap root. For example a default sitemap and a news
specific sitemap, that contains only 1000 urls that were changed in the past 2 days. Or as another example would a
product sitemap for each of a catalogues top level categories. To enable that, a `SitemapGenerator` can return _0..n_
names for a given resource, each name representing a single sitemap at the given resource.

`SitemapGenerator` implementations need to be registered as OSGI services. In case of an overlap, it depends on
the `service.ranking` which `SitemapGenerators` are used for a particular sitemap root. For example, it may be that two
`SitemapGenerators` can generator a default sitemap for a sitemap root, but the second one can also generate a news
sitemap. The first, higher-ranked `SitemapGenerator` will be used for the default sitemap, but the second will still be
taken into account for the news sitemap.

#### Background Generation

Background generation is triggered by a configurable scheduler. Multiple schedules can be created for different sitemap
names as described before. This enables the background generation to align on the cadence in which particular content
may be updated, for example product catalogues that sync on a regular basis.

For each sitemap root in the repository and for each sitemap name returned for them a job will be queued. It is
recommended to create an unordered queue for those jobs so that they can be distributed across multiple instances within
a cluster.

```
sling/sitemap/build
 + sitemap.root = /content/site/ch/de-ch
 + sitemap.name = <default>
```

The `SitemapGeneratorExecutor` provides an execution context to the `SitemapGenerator`, that it may use to keep track on
the progress. The implementation on the other hand will persist this state along with the already written sitemap after
a configurable amount of urls has been added. This helps to resume jobs after an instance gets restarted or discarded in
a dynamic cluster, and the jobs gets reassigned to another instance. Per default the `SitemapGeneratorExecutor` is
configured with a chunk size of `Integer.MAX_VALUE`, which effectively means that no checkpoints will be written. When
using this feature make sure to find a good balance between write overhead and performance gain achieved for those
particular cases.

#### On-demand Generation

For smaller sites, calculating sitemaps in the background may not be necessary and serving sitemaps when they get
requested may even result in higher accuracy. On the other hand it highly depends on the amount of content and
the `SitemapGeneator` implementation(s) used, if it is possible to serve a sitemap within the timeout of the different
crawlers. Because of that, serving sitemaps on-demand must be explicitly enabled.

To configure serving sitemaps on-demand, set the `onDemandNames` of the `SitemapServiceImpl`. When set, the
`SitemapServlet` slightly changes its behaviour. The sitemap index is now served based on a query of all sitemap roots
below the requested resource to check if any of them generates a sitemap for the on-demand names, instead of simply
creating an index of all stored sitemaps at the requested sitemap root. For the sitemaps the sitemap selector will be
resolved to sitemap-root-name pairs to check if any of them can be served on-demand, instead of serving a file from the
storage. However in either of the cases the mechanism falls back to the sitemaps generated in the background.

### Sitemap Extensions

The builder-like API supports adding sitemap extensions (image, video, news, alternate languages ...) by implementing an
`ExtensionProvider`. This provider has to be registered specifying `namespace`, `prefix`, and `localName` of the xml
element the extension adds to an url object.

In order to hide the implementation detail from the consumer API, the `ExtensionProvider` works with an abstract
`AbstractExtension` class, and the consumer API only with an `Extension` marker interface. To implement an Extension:

* Extend the marker interface with methods to capture the extension specific data
* Implement this extension interface by extending the `AbstractExtension` class
* Provide an instance of the extension implementation by implementing an `ExtensionProvider`
* Make sure to register the `ExtensionProvider` with the `extension.interface` set to the fqn of the extension interface

An example extension implementation can be found with
the [AlternateLanguageExtension](src/main/java/org/apache/sling/sitemap/builder/extensions/AlternateLanguageExtension.java)
.