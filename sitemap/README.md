[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

# Apache Sling Sitemap

The Apache Sling Sitemap module is an extension for Apache Sling that helps to serve XML Sitemaps to Search Engines. It
was designed to cover various uses cases from small sites serving sitemaps on-demand, large sites generating them in the
background to even sites that collect 3rd party data to include dynamically rendered pages.

## Highlights

* A simple, builder-like API to create Sitemaps, that hides all the XML specifics from the implementation
* Support for on-demand and background generation w/ continuation after job interruption
* Support for nested sitemaps, that are automatically collected into a sitemap indexes
* Support for auto-balancing sitemaps into multiple files for background generation

## Open Topics

* Implement Google specific sitemap extensions (image/video/news)

## Getting Started

The Sling Sitemap module is rather abstract, as it highly depends on the content structure of the application it is 
used in. To get started a few things must be done:

1) Implement at least one `SitemapGenerator`. The abstract `ResourceTreeSitemapGenator` may be a good starting
   point, for any generator walking the resource tree. 
2) Configure the `SitemapServlet` to register for the resource type(s) which may be a sitemap root resource.
3) Configure a service user mapping for `org.apache.sling.sitemap:sitemap-reader` granting read access to the content
4) Configure another service user mapping one for `org.apache.sling.sitemap:sitemap-writer` granting read access to the 
   content and write access to the storage path for background generation (per default /var/sitemaps)
5) Finally, either configure a `SitemapScheduler` to create a job for background generation, or implement the 
   `SitemapGenerator` to serve the sitemaps on-demand.
   
## Implementation Details

### Content Model

In order to serve a sitemap, a resource must be marked as sitemap root resource. This is done by adding
a `sling:sitemapRoot = true` property either to the resource, or it's `jcr:content` child.

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

A configurable scheduler can be configured to trigger background generation. Multiple schedules can be created for
different sitemap names or generators as described before. This enables the background generation to align on the
cadence in which particular content may be updated, for example product catalogues that sync on a regular basis.

For each sitemap root in the repository and for each sitemap name returned for them, a job with the topic
`org/apache/sling/sitemap/build` will be queued. The `SitemapGeneratorExecutor` implementation consumes those jobs and
calls the corresponding `SitemapGeneator`. It is recommended to create an unordered queue for those jobs so that they
can be distributed across multiple instances within a cluster.

The `SitemapGeneratorExecutor` provides an execution context to the `SitemapGenerator`, that it may use to keep track on
the progress. The implementation on the other hand will persist this state along with the already written sitemap after
a configurable amount of urls has been added. This allows to resume jobs after an instance gets restarted or discarded
in a dynamic cluster. Per default the `SitemapGeneratorExecutor` is configured with a chunk size of `Integer.MAX_VALUE`,
which effectively means that no checkpoints will be written. When using this feature make sure to find a good balance
between write overhead and performance gain for those particular cases.

Background generation supports auto-balancing according to configurable limits for size (in bytes), and the number of
urls in a single sitemap file. This is transparently handled by the `SitemapGeneatorExecutor`, providing a `Sitemap`
instance which pipes added urls to multiple files when needed. As a consequence returning sitemap files from storage for
a given name and sitemap root may result in multiple return values.

#### On-demand Generation

For smaller sites, calculating sitemaps in the background may not be necessary and serving sitemaps when they get
requested may even result in higher accuracy. On the other hand serving a sitemap on-demand within the timeout of
different crawlers highly depends on the amount of content and the `SitemapGeneator` implementation(s) used. Because of
that, serving sitemaps on-demand must be explicitly enabled.

To enable serving sitemaps on-demand, a `SitemapGenerator` must indicate that a particular sitemap name should be served
on demand. Alternatively the `SitemapGeneatorManagerImpl` can be configured to force all sitemaps to be served
on-demand. In both cases, the `SitemapServlet` changes its behaviour slightly:

- When serving a sitemap-index, it queries for all sitemap roots and adds the sitemaps of those, that should be served
  on-demand. Additionally, all sitemaps form the top level sitemap root's storage location are added if not already
  present.
- For serving sitemap files it first checks if the sitemap should be served on-demand, if not it falls back to the
  sitemap file from the top level sitemap root's storage location.

**On-demand generation does NOT support auto-balancing. The configured limits will be ignored.**

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