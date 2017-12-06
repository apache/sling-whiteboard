# An Introduction to the Feature Model

Typical OSGi applications are assembled out of bundles and configured through both, OSGi configurations and framework properties. Depending on the nature of the application, there might be additional artifact types involved.

While bundles already provide a good way to define rather small, coherent modules, there is often a need to distribute or provision a set of such bundles together with some configuration. OSGi Deployment Admin and OSGi subsystems are two ways of trying to solve this issue. The feature model of Apache Karaf and the provisioning model of Apache Sling are two other approaches.

The goal of this proposal is to define a common way of describing such features and how features are combined to either create higher level features or an OSGi applications.

# Requirements

The feature model should at least meet the following requirements:

* A feature model must have a unique identifier
* A feature model must have a version
* The feature model should be described through a text format which is easily consumable by both humans and machines
* It must be possible to list the bundles belonging to the feature
* It must be possible to list the OSGi configurations for this features
* It must be possible to associate an OSGi configuration with a bundle within a features
* It must be possible to define framework properties
* The feature model must be extensible to allow other (proprietary) Artifacts
* The feature model must describe how several features are aggregated to build a higher level feature
* The feature model must describe how several features are aggregated to build an application
* A feature must be able to specify additional requirements and capabilities that extend the requirements and capabilities from the contained artifacts.
* More...

# Prototype for a new Provisioning / Configuration Model for OSGi based applications

The current model to describe OSGi applications is based on Apache Sling's provisioning model (see https://sling.apache.org/documentation/development/slingstart.html)

## Short description of Sling's provisioning model:

* Text based file format, defining features (several in a single file)
* A feature can have a name and version (both optional)
* A feature consists of sections, well defined ones like the run modes and user defined sections
* A run mode has artifacts (with start levels), configurations and settings (framework properties)
* Variables can be used throughout a feature
* Inheritance is supported on a feature base through artifacts
* Configuration merging is possible

## Advantages of the provisioning model

* Well known by Sling developers, has been introduced some years ago. Some tooling around it
* Very concise, especially for defining artifacts
* Extensible, custom sections can be added, e.g used by Sling for repoinit, subsystem definitions, content package definitions
* Easy diff
* Special API with semantics related to the prov model (not a general purpose config API)

## Disadvantages of the provisioning model

* Single file can contain more than one feature
* Custom DSL - no standard format (like JSON)
* Inheritance and custom artifacts (content packages) are mixed with bundles, which makes processing and understanding more complicated
* Adding additional info to artifacts looks strange
* Two formats for configurations and now there is an official JSON based format defined through OSGi R7
* Strange object relationship between feature and run modes
* API (object relation) differs from text file (to make the text format easier)
* Tooling only available as maven plugins, not separate usable
* Run mode handling is complicating the feature files and processing of those
* Tightly coupled with the way Sling's launchpad works, therefore no independent OSGi format

# Design Criteria for a model

* A feature is a separate file with a required name and version
* A feature can include other features
* No support for run modes in the model - run modes can be modeled through separate features
* OSGi JSON format for configurations
* Support for standard OSGi artifacts as well as custom artifacts (like content packages)
* Support OSGi requirements and capabilities for dependency handling

# Prototype

The prototype uses JSON as a well defined and understood format. This fits nicely with the new OSGi R7 JSON format for configurations.

A model file describes a feature. A feature consists of:
* A unique id and version (see Feature Identity below)
* A list of bundles described through maven coordinates
  * Grouped by start level (required)
  * Additional metadata like a hash etc. (optional)
  * Configurations (optional)
* A set of global configurations
* A set of framework properties
* A list of provided capabilities
* A list of required capabilities
* A set of includes (of other features) described through maven coordinates
  * Modifications (removals) of the includes (optional)
* Extensions (optional)
  * A list of repoinit instructions
  * A set of content packages described through maven coordinates
    * Additional metadata like a hash etc. (optional)
    * Configurations (optional)

# Feature Identity

A feature is uniquely identified through maven coordinates, it has a group id, an artifact id and a version. In addition it might have a classifier.

TBD We need to define a common type for such a feature. It could be "osgifeature" (but is this a good type? slingfeature, slingstart are taken, osgifeature might be too general)

# Maven coordinates

Maven coordinates are used to describe artifacts, e.g. bundles, content packages or includes. In these cases either the short notation (as described here: https://maven.apache.org/pom.html#Maven_Coordinates) can be used or the long version as a JSON object with an id property.

# Requirements and Capabilities vs Dependencies

In order to avoid a concept like "Require-Bundle" a feature does not explicitely declare dependencies to other features. These are declared by the required capabilities, either explicit or implicit. The implicit requirements are calculated by inspecting the contained bundles (and potentially other artifacts like content packages ).

Once a feature is processed by tooling, the tooling might create a full list of requirements and capabilities and add this information in a special section to the final feature. This information can be used by tooling to validate an instance (see below) and avoids rescanning the binary artifacts. However this "cached" information is optional and tooling must work without it (which means it needs access to the binaries in that case). TBD the name and format of this information.

# Includes

Includes allow an aggregation of features and a modification of the included feature: each entity listed in the included feature can be removed, e.g a configuration or a bundle. The list of includes must not contain duplicates (not comparing the version of the includes). If there are duplicates, the feature is invalid.

Once a feature is processed, included references are removed and the content of the included features becomes part of the current feature. The following algorithm applies:

* Includes are processed in the order they are defined in the model. The current feature (containing the includes) is used last which means the algorithm starts with the first included feature.
* Removal instructions for an include are handled first
* A clash of bundles or content packages is resolved by picking the latest version (not the highest!)
* Configurations will be merged by default, later ones potentially overriding newer ones:
  * If the same property is declared in more than one feature, the last one wins - in case of an array value, this requires redeclaring all values (if they are meant to be kept)
  * Configurations can be bound to a bundle. When two features are merged, all cases can occur: both might be bound to the same bundle (symbolic name), both might not be bound, they might be bound to different bundles (symbolic name), or one might be bound and the other one might not. As configurations are handled as a set regardless of whether they are bound to a bundle or not, the information of the belonging bundle is handled like a property in the configuration. This means:
    * If the last configuration belongs to a bundle, this relationship is kept
    * If the last configuration does not belong to a bundle and has no property removal instruction, the relationship from the first bundle is used (if there is one)
    * If the last configuration has a property removal instruction for the bundle relationship, the resulting configuration is unbound
* Later framework properties overwrite newer ones
* Capabilities and requirements are appended - this might result in duplicates, but that doesn't really hurt in practice.
* Extensions are handled in an extension specific way:
    * repoinit is just aggregated (appended)
    * artifact extensions are handled like bundles

While includes must not be used for assembling an application, they provide an important concept for manipulating existing features. For example to replace a bundle in an existing feature and deliver this modified feature.

# Extensions

An extension has a unique name and a type which can either be text, JSON or artifacts. Depending on the type, inheritance is performed like this:
* For type text: simple appended
* For type JSON: merging of the JSON structure, later arriving properties overriding existing ones
* For type artifacts: merging of the artifacts, higher version wins

# Handling of Environments

A feature itself has no special support for environments (prod, test, dev). In practice it is very unlikely that a single file exists containing configurations for all environments, especially as the configuration might contain secrets, credentials, urls for production services etc which are not meant to be given out in public (or to the dev department). Instead, a separate feature for an environment can be written and maintained by the different share holders which adds the environment specific configuration. Usually this feature would include the feature it is based on.

# Bundles and start levels

Each bundle needs to be explicitly assigned to a start level. There is no default start level as a default start level is not defined in the OSGi spec. In addition, it is a little bit confusing when looking at the model when there is a list of bundles without a start level. Which start level do these have? It is better to be explicit.

However as soon as you have more than one feature and especially if these are authored by different authors, start level handling becomes more tricky. Assigning correct OSGi start levels in such scenarios would require to know all features upfront. Therefore this start level information is interpret as follows: instead of directly mapping it to a start level in the OSGi framework, it defines just the startup order of bundles within a feature. Features are then started in respect of their dependency information. Even if a feature has no requirement with respect to start ordering of their bundles, it has to define a start level (to act as a container for the bundles). It can use any positive number, suggested is to use "1". Bundles within the same start level are started in any order.

# Configurations belonging to Bundles

In most cases, configurations belong to a bundle. The most common use case is a configuration for a (DS) component. Therefore instead of having a separate configurations section, it is more intuitiv to specify configurations as part of a bundle. The benefit of this approach is, that it can easily be decided if a configuration is to be used: if exactly that bundle is used, the configurations are used; otherwise they are not.

However, there might be situations where it is not clear to which bundle a configuration belongs or the configuration might be a cross cutting concern spawning across multiple bundles. Therefore it is still possible to have configurations not related to a particular bundle.

In fact, configurations - whether they are declared as part of a bundle or not - are all managed in a single set for a feature. See above for how includes etc. are handled.

# Example

This is a feature example:

    {
      "id" : "org.apache.sling:my.app:feature:optional:1.0",

      "includes" : [
         {
             "id" : "org.apache.sling:sling:9",
             "removals" : {
                 "configurations" : [
                 ],
                 "bundles": [
                 ],
                 "framework-properties" : [
                 ]
             }
         }
      ],
      "requirements" : [
          {
              "namespace" : "osgi.contract",
              "directives" : {
                  "filter" : "(&(osgi.contract=JavaServlet)(version=3.1))"
              }
          }
      ],
      "capabilities" : [
        {
             "namespace" : "osgi.implementation",
             "attributes" : {
                   "osgi.implementation" : "osgi.http",
                   "version:Version" : "1.1"
             },
             "directives" : {
                   "uses" : "javax.servlet,javax.servlet.http,org.osgi.service.http.context,org.osgi.service.http.whiteboard"
             }
        },
        {
             "namespace" : osgi.service",
             "attributes" : {
                  "objectClass:List<String>" : "org.osgi.service.http.runtime.HttpServiceRuntime"
             },
             "directives" {
                  "uses" : "org.osgi.service.http.runtime,org.osgi.service.http.runtime.dto"
             }
        }
      ],
      "framework-properties" {
        "foo" : 1,
        "brave" : "something",
        "org.apache.felix.scr.directory" : "launchpad/scr"
      },
      "bundles" : {
        "1" : [
            {
              "id" : "org.apache.sling:security-server:2.2.0",
              "hash" : "4632463464363646436"
            },
            "org.apache.sling:application-bundle:2.0.0",
            "org.apache.sling:another-bundle:2.1.0"
          ],
        "2" : [
            "org.apache.sling:foo-xyz:1.2.3"
          ]
      },
      "configurations" {
        "my.pid" {
           "foo" : 5,
           "bar" : "test",
           "number:Integer" : 7
        },
        "my.factory.pid~name" {
           "a.value" : "yeah"
        }
    }

# Relation to Repository Specification (Chapter 132)

There are two major differences between a repository as described in the Repository Service Description and the feature model. A repository contains a list of more or less unrelated resources whereas a feature describes resources as a unit. For example a feature allows to define a bundle together with OSGi configurations - which ensures that whenever this feature is used, the bundle *together with* the configurations are deployed. A repository can only describe the bundle as a separate resource and the OSGi configurations as additional unrelated resources.

The second difference is the handling of requirements and capabilities. While a repository is supposed to list all requirements and capabilities of a resource as part of the description, the feature model does not require this. As the feature model refers to the bundle and the bundle has the requirements and capabilities as metadata, there is no need to repeat that information.

By these two differences you can already tell, that a repository contents is usually generated by tools while a feature is usually a human created resource. While it is possible to create a repository index out of a feature, the other way round does not work as the repository has no standard way to define relationships between resources.

# Requirements and Capabilities of Artifacts

The feature model does not allow to explicitly list requirements or capabilities for artifacts. An artifact, for example a bundle, contains this information as part of its metadata. However, to calculate or display these, the tool processing the feature needs to have access to the artifact and needs to extract this. While in general this does not pose a problem by itself, it might happen that the same artifact is processed several times for example during a build process, causing overhead.

To avoid this, a feature might contain an additional section, named "reqscaps" (TODO find a better name). This section is grouped by artifact ids and contains the requirements and capabilities of each artifact. While the requirements and capabilities of a single artifact must be correct and neither leave out or add additional ones, the list of artifacts must not be complete. Tooling will first look into this section to get requirements and capabilities for an artifact. If there are none, it will process the artifact.

    {
        ...
        "reqscaps" : {
            "org.apache.sling:org.apache.sling.scripting.jsp:1.0.0" : {
                "capabilities" : [],
                "requirements" : []
            }
        }


# Provisioning Applications

An application jar can contain a set of features (including the listed artifacts).

An optional application configuration further defines the possibilites:

    {
         "features" : [
             "org.apache.sling:org.apache.sling.launchpad:10"
         ],
         "options" : [
             "org.apache.sling:org.apache.sling.scripting.jsp:1.0.0",
             {
                 "id" : "org.apache.sling:org.apache.sling.scripting.htl:1.0.0",
                 "tag": "htl"
             }
         ],
         "defaults" : {
             "auto-add-options": true,
             "tags" : ["htl"]
         },
         "framework" : {
             "id" : "org.apache.felix:org.apache.felix.framework:5.6.4"
         }
    }

Such a configuration is required for an application, at least one feature needs to be listed in either the features or the options section.
All features listed in the features section will be added to the application, the ones listed in options are optional and depending on the settings and user input will either be added or left out. In addition all available features of an application will be used to make the application runnable (resolvable).
