# Apache Sling Content-Package to Feature Model converter

This tool aims to provide to Apache Sling users an easy-to-use conversion tool which is able to convert `content-package` archives to the new _Sling Feature Model_.

## Introduction

`content-package`s are zipped archives containing OSGi bundles, OSGi configurations and resources (and nested `content-package`s as well), aside metadata, that can be used to install content into a _JCR_ repository using the [Apache Jackrabbit FileVault](http://jackrabbit.apache.org/filevault/) packaging runtime.

OTOH, [Apache Sling Feature](https://github.com/apache/sling-org-apache-sling-feature) allows users to describe an entire OSGi-based application based on reusable components and includes everything related to this application, including bundles, configuration, framework properties, capabilities, requirements and custom artifacts.

The _Apache Sling Content Package to Feature Model converter_ (referred as _cp2fm_) is a tool able to extract OSGI bundles, OSGi configurations, resources and iteratively scan nested `content-package`s from an input `content-package` and create one (or more) _Apache Sling Feature_ model files and deploy the extracted OSGi bundles in a directory which structure is compliant the _Apache Maven_ repository conventions.

## Understanding the Input

As exposed above, `content-package`s are archives, compressed with the ZIP algorithm, which contain:

 * OSGi bundles, conventionally found under the `jcr_root/apps/<application>/install(.runMode)/<bundle>.jar` path; typically, OSGi bundles are also valid _Apache Maven_ artifacts, that means that they contain _Apache Maven_ metadata files such as `META-INF/maven/<groupId>/<artifactId>/pom.(xml|properties)`;
 * OSGi configurations, conventionally found under the `jcr_root/apps/<application>/config(.runMode)/<configuration>.<extension>` path;
 * nested `content-package`s, conventionally found under the `jcr_root/etc/packages/<package-name>.zip` path;
 * Metadata files, under the `META-INF/` directory;
 * any other kind of resource.

### a content-package sample

We can have a look at what's inside a `test-content-package.zip` test `content-package` included in the `cp2fm` test resources:

```
$ unzip -l ./content-package-2-feature-model/src/test/resources/org/apache/sling/cp2fm/test-content-package.zip 
Archive:  content-package-2-feature-model/src/test/resources/org/apache/sling/cp2fm/test-content-package.zip
  Length      Date    Time    Name
---------  ---------- -----   ----
        0  03-12-2019 17:31   META-INF/
       69  03-12-2019 17:31   META-INF/MANIFEST.MF
        0  03-12-2019 17:06   jcr_root/
        0  03-12-2019 17:06   jcr_root/etc/
        0  03-12-2019 17:06   jcr_root/etc/packages/
        0  03-12-2019 17:30   jcr_root/etc/packages/asd/
    34493  03-12-2019 17:30   jcr_root/etc/packages/asd/test-bundles.zip
     8333  03-12-2019 17:09   jcr_root/etc/packages/asd/test-content.zip
     7235  03-12-2019 17:08   jcr_root/etc/packages/asd/test-configurations.zip
        0  03-12-2019 15:28   META-INF/maven/
        0  03-12-2019 15:29   META-INF/maven/org.apache.sling/
        0  02-28-2019 14:27   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.all/
     1231  03-12-2019 15:30   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.all/pom.xml
      127  03-12-2019 15:30   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.all/pom.properties
        0  03-12-2019 17:06   META-INF/vault/
      892  03-12-2019 15:32   META-INF/vault/settings.xml
      840  03-12-2019 15:47   META-INF/vault/properties.xml
     3579  03-12-2019 15:33   META-INF/vault/config.xml
      267  03-12-2019 15:50   META-INF/vault/filter.xml
---------                     -------
    63214                     20 files
```

Where the `test-bundles.zip` is a nested `content-package` wrapping OSGi bundles:

```
$ unzip -l test-bundles.zip 
Archive:  test-bundles.zip
  Length      Date    Time    Name
---------  ---------- -----   ----
        0  03-12-2019 17:30   META-INF/
       69  03-12-2019 17:30   META-INF/MANIFEST.MF
        0  03-11-2019 23:39   jcr_root/
        0  03-11-2019 23:31   jcr_root/apps/
        0  03-12-2019 17:26   jcr_root/apps/asd/
        0  03-11-2019 23:32   jcr_root/apps/asd/install/
    13288  12-06-2018 12:30   jcr_root/apps/asd/install/test-framework.jar
        0  03-12-2019 17:16   jcr_root/apps/asd/install.publish/
     7210  03-12-2019 17:15   jcr_root/apps/asd/install.publish/test-api.jar
        0  03-12-2019 17:18   jcr_root/apps/asd/install.author/
     7735  03-12-2019 17:17   jcr_root/apps/asd/install.author/test-api.jar
        0  03-11-2019 23:42   META-INF/maven/
        0  03-11-2019 23:43   META-INF/maven/org.apache.sling/
        0  02-28-2019 14:26   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.bundles/
     1229  03-12-2019 10:22   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.bundles/pom.xml
      131  03-12-2019 00:26   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.bundles/pom.properties
        0  03-12-2019 12:41   META-INF/vault/
      888  03-12-2019 00:28   META-INF/vault/settings.xml
      954  03-12-2019 15:33   META-INF/vault/properties.xml
     3571  03-12-2019 00:27   META-INF/vault/config.xml
      891  03-12-2019 00:28   META-INF/vault/filter.xml
      842  03-12-2019 00:27   META-INF/vault/filter-plugin-generated.xml
---------                     -------
    79844                     29 files
```

the `test-configurations.zip` contains OSGi configurations:

```
$ unzip -l test-configurations.zip 
Archive:  test-configurations.zip
  Length      Date    Time    Name
---------  ---------- -----   ----
        0  03-12-2019 17:08   META-INF/
       69  03-12-2019 17:08   META-INF/MANIFEST.MF
        0  03-12-2019 10:21   META-INF/maven/
        0  03-12-2019 10:21   META-INF/maven/org.apache.sling/
        0  02-28-2019 14:25   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.config/
     1228  03-12-2019 10:24   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.config/pom.xml
      129  03-12-2019 10:22   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.config/pom.properties
        0  03-12-2019 13:23   META-INF/vault/
       94  02-28-2019 14:25   META-INF/vault/settings.xml
      664  03-12-2019 15:13   META-INF/vault/properties.xml
     3579  02-28-2019 14:25   META-INF/vault/config.xml
      175  03-12-2019 10:37   META-INF/vault/filter.xml
        0  02-28-2019 14:25   jcr_root/
        0  03-12-2019 10:17   jcr_root/apps/
        0  02-28-2019 14:25   jcr_root/apps/asd/
        0  03-12-2019 10:17   jcr_root/apps/asd/config/
      438  02-28-2019 14:25   jcr_root/apps/asd/config/org.apache.sling.commons.log.LogManager.factory.config-asd-retail.xml
        0  03-12-2019 10:18   jcr_root/apps/asd/config.publish/
      377  02-28-2019 14:25   jcr_root/apps/asd/config.publish/org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-asd-retail.xml
      244  02-28-2019 14:25   jcr_root/apps/.content.xml
---------                     -------
    25441                     23 files
```

and the `test-content.zip` package includes resources of various nature:

```
$ unzip -l test-content.zip 
Archive:  test-content.zip
  Length      Date    Time    Name
---------  ---------- -----   ----
        0  03-12-2019 17:09   META-INF/
       69  03-12-2019 17:09   META-INF/MANIFEST.MF
        0  03-12-2019 11:31   META-INF/maven/
        0  03-12-2019 11:31   META-INF/maven/org.apache.sling/
        0  02-28-2019 14:26   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.content/
     1229  03-12-2019 11:32   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.content/pom.xml
      131  03-12-2019 11:32   META-INF/maven/org.apache.sling/org.apache.sling.cp2fm.content/pom.properties
        0  03-12-2019 12:40   META-INF/vault/
      118  02-28-2019 14:26   META-INF/vault/settings.xml
      859  03-12-2019 15:12   META-INF/vault/properties.xml
     3571  03-12-2019 12:42   META-INF/vault/config.xml
      895  03-12-2019 12:57   META-INF/vault/filter.xml
       72  02-28-2019 14:26   META-INF/vault/filter-plugin-generated.xml
        0  03-12-2019 12:30   jcr_root/
        0  03-12-2019 12:31   jcr_root/content/
        0  03-12-2019 12:31   jcr_root/content/asd/
     1021  02-28-2019 14:26   jcr_root/content/asd/.content.xml
     6924  02-28-2019 14:26   jcr_root/content/asd/resources.xml
---------                     -------
    39481                     22 files
```

## Mapping and the Output

All metadata are mainly collected inside one or more, depending by declared run modes in the installation and configuration paths, _Feature_ model files:

```json
$ cat asd.retail.all.json 
{
  "id":"org.apache.sling:asd.retail.all:slingosgifeature:cp2fm-converted-feature:0.0.1",
  "description":"Combined package for asd.Retail",
  "bundles":[
    {
      "id":"org.apache.felix:org.apache.felix.framework:6.0.1",
      "start-order":"5"
    }
  ],
  "configurations":{
    "org.apache.sling.commons.log.LogManager.factory.config-asd-retail":{
      "org.apache.sling.commons.log.pattern":"{0,date,yyyy-MM-dd HH:mm:ss.SSS} {4} [{3}] {5}",
      "org.apache.sling.commons.log.names":[
        "we.retail"
      ],
      "org.apache.sling.commons.log.level":"info",
      "org.apache.sling.commons.log.file":"logs/project-we-retail.log"
    }
  },
  "content-packages:ARTIFACTS|true":[
    "org.apache.sling:asd.retail.all:zip:cp2fm-converted-feature:0.0.1"
  ]
}
```

the `publish` run mode leads the tool to generate a separated _Apache Sling Feature_ model file:

```json
$ cat asd.retail.all-publish.json 
{
  "id":"org.apache.sling:asd.retail.all:slingosgifeature:cp2fm-converted-feature-publish:0.0.1",
  "bundles":[
    {
      "id":"org.apache.sling:org.apache.sling.models.api:1.3.8",
      "start-order":"5"
    }
  ],
  "configurations":{
    "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-asd-retail":{
      "user.mapping":[
        "com.asd.sample.we.retail.core:orders=[commerce-orders-service]",
        "com.asd.sample.we.retail.core:frontend=[content-reader-service]"
      ]
    }
  }
}
```

bundles are collected in an _Apache Maven repository_ compliant directory, all other resources are collected in a new `content-package` created while scanning the packages:

```
$ tree bundles/
bundles/
└── org
    └── apache
        ├── felix
        │   └── org.apache.felix.framework
        │       └── 6.0.1
        │           ├── org.apache.felix.framework-6.0.1.jar
        │           └── org.apache.felix.framework-6.0.1.pom
        └── sling
            ├── asd.retail.all
            │   └── 0.0.1
            │       ├── asd.retail.all-0.0.1-cp2fm-converted-feature.zip
            │       └── asd.retail.all-0.0.1.pom
            ├── org.apache.sling.api
            │   └── 2.20.0
            │       ├── org.apache.sling.api-2.20.0.jar
            │       └── org.apache.sling.api-2.20.0.pom
            └── org.apache.sling.models.api
                └── 1.3.8
                    ├── org.apache.sling.models.api-1.3.8.jar
                    └── org.apache.sling.models.api-1.3.8.pom

12 directories, 8 files
```

_Apache Maven GAVs_ are extracted from nested bundles metadata and are renamed according to the _Apache Maven_ conventions.

### Supported configurations

All generally adopted OSGi configuration formats are supported:

 * _Property_ files, which extensions are `.properties` or `.cfg`, see the related [documentation](https://sling.apache.org/documentation/bundles/configuration-installer-factory.html#property-files-cfg);
 * Configuration Files, which extension is `.config`, see the related [documentation](https://sling.apache.org/documentation/bundles/configuration-installer-factory.html#configuration-files-config);
 * JSON format, which extension is `.cfg.json`, see the related [documentation](https://blog.osgi.org/2018/06/osgi-r7-highlights-configuration-admin.html)
 * `sling:OsgiConfig` content nodes, typically `.xml` files.

### Run Modes

As shown above, run modes in the path lead the tool to create a dedicated _Apache Sling Feature_ model file containing all interested OSGi configurations/bundles.

### Known limitations

Multiple Run Modes are not supported yet.

## Sample APIs

```java
import org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter;

...

new ContentPackage2FeatureModelConverter()
            // content-package validation, when opening them
            .setStrictValidation(strictValidation)
            // (don't) allow different OSGi configurations file have the same PID
            .setMergeConfigurations(mergeConfigurations)
            // users can decide which is the bundles start order, declared in the generated Apache Sling Feature(s)
            .setBundlesStartOrder(bundlesStartOrder)
            // a valid directory where the outputs will be generated (it will created, if not existing already)
            .setOutputDirectory(outputDirectory)
            // an existing and valid content-package file
            .convert(contentPackage);
```

### Handler Services

In order to make the tool extensible, the `org.apache.sling.cp2fm.spi.EntryHandler` interface is declared to handle different kind of resources, have a look at the `org.apache.sling.cp2fm.handlers` package to see the default ones.

If users want to handle special resource type, all they have to do is providing their implementation and declaring it under the `META-INF/services/org.apache.sling.cp2fm.spi.EntryHandler` classpath resource file, on order to let the `ServiceLoader` including it in the `content-package` scan.

## The CLI Tool

The tool is distributed with a commodity package containing all is needed in order to launch the `ContentPackage2FeatureModelConverter` form the shell:

```
$ unzip -l org.apache.sling.cp2fm-0.0.1-SNAPSHOT.zip 
Archive:  org.apache.sling.cp2fm-0.0.1-SNAPSHOT.zip
  Length      Date    Time    Name
---------  ---------- -----   ----
        0  03-13-2019 15:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/
        0  03-13-2019 15:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/bin/
        0  03-13-2019 15:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/
     4605  02-27-2019 16:30   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/README.md
   801904  02-28-2019 14:55   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/jackrabbit-spi-commons-2.19.1.jar
    14744  02-11-2019 15:44   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/osgi.annotation-6.0.1.jar
    35919  02-11-2019 15:44   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.osgi.service.component.annotations-1.3.0.jar
    23575  02-11-2019 15:44   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.osgi.service.metatype.annotations-1.3.0.jar
    34518  02-27-2019 15:28   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.apache.felix.scr.annotations-1.11.0.jar
    45199  03-13-2019 15:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.apache.sling.cp2fm-0.0.1-SNAPSHOT.jar
    17489  03-13-2019 15:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/LICENSE
   588337  02-11-2019 12:49   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/commons-collections-3.2.2.jar
   108555  02-11-2019 15:45   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/xz-1.8.jar
    52873  03-05-2019 17:31   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/plexus-classworlds-2.6.0.jar
   165965  03-05-2019 18:02   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/maven-model-3.6.0.jar
      178  02-27-2019 15:56   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/NOTICE
   745712  02-28-2019 10:02   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.apache.jackrabbit.vault-3.2.6.jar
  2374421  02-27-2019 15:28   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/biz.aQute.bndlib-3.2.0.jar
     3263  03-13-2019 15:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/bin/cp2sf.bat
    69246  02-11-2019 12:49   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/jcr-2.0.jar
   113508  02-11-2019 12:36   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.apache.felix.converter-1.0.0.jar
    12548  02-11-2019 12:36   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.osgi.util.function-1.0.0.jar
   176142  02-11-2019 12:35   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.apache.felix.utils-1.11.0.jar
   155618  03-04-2019 00:12   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.apache.felix.configadmin-1.9.12.jar
    75443  03-05-2019 14:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/plexus-io-3.1.1.jar
    57954  02-11-2019 12:39   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/snappy-0.4.jar
   148098  02-11-2019 12:39   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/xbean-reflect-3.7.jar
     3808  03-13-2019 15:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/bin/cp2sf
   214788  02-11-2019 15:44   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/commons-io-2.6.jar
    26081  02-11-2019 12:36   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/geronimo-json_1.0_spec-1.0-alpha-1.jar
    90358  02-11-2019 12:35   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/johnzon-core-1.0.0.jar
    14769  02-11-2019 12:35   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.osgi.annotation.versioning-1.0.0.jar
   475256  02-11-2019 12:35   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/osgi.core-6.0.0.jar
    28688  02-11-2019 12:48   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/slf4j-api-1.7.6.jar
    28561  02-28-2019 14:55   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/jackrabbit-spi-2.19.1.jar
   403186  02-28-2019 14:55   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/jackrabbit-jcr-commons-2.19.1.jar
    49017  03-04-2019 15:12   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/jackrabbit-api-2.19.1.jar
   260371  03-05-2019 14:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/plexus-utils-3.1.1.jar
   639592  02-11-2019 12:39   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/google-collections-1.0.jar
    10684  02-11-2019 12:48   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/slf4j-simple-1.7.6.jar
   164159  02-11-2019 12:48   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.apache.sling.feature.io-1.0.0.jar
   289040  02-11-2019 12:36   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.apache.felix.configurator-1.0.4.jar
   591748  02-11-2019 15:45   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/commons-compress-1.18.jar
   242435  02-27-2019 15:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/picocli-3.6.0.jar
   115238  02-11-2019 12:48   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/org.apache.sling.feature-1.0.0.jar
    18587  02-11-2019 15:46   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/annotations-16.0.3.jar
   191914  03-05-2019 14:58   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/plexus-archiver-4.1.0.jar
   229982  03-05-2019 17:31   org.apache.sling.cp2fm-0.0.1-SNAPSHOT/lib/plexus-container-default-2.0.0.jar
---------                     -------
  9914076                     48 files
```

once the package is decompressed, open the shell and type:

```
$ ./bin/cp2sf -h
Usage: cp2fm [-hmqsvX] [-b=<bundlesStartOrder>] -c=<contentPackage>
             -o=<outputDirectory>
Apache Sling Content Package to Sling Feature converter
  -b, --bundles-start-order=<bundlesStartOrder>
                            The order to start detected bundles.
  -c, --content-package=<contentPackage>
                            The content-package input file.
  -h, --help                Display the usage message.
  -m, --merge-configurations
                            Flag to mark OSGi configurations with same PID will be
                              merged, the tool will fail otherwise.
  -o, --output-directory=<outputDirectory>
                            The output directory where the Feature File and the
                              bundles will be deployed.
  -q, --quiet               Log errors only.
  -s, --strict-validation   Flag to mark the content-package input file being strict
                              validated.
  -v, --version             Display version information.
  -X, --verbose             Produce execution debug output.
Copyright(c) 2019 The Apache Software Foundation.
```

to see all the available options; a sample execution could look like:

```
$ ./bin/cp2sf -v -b 20 -c /content-package-2-feature-model/src/test/resources/org/apache/sling/cp2fm/test-content-package.zip -o /tmp
```
