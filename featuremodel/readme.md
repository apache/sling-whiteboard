# An Introduction to the Feature Model

Typical OSGi applications are assembled out of bundles and configured through both, OSGi configurations and framework properties (though these less frequently used than OSGi configurations). Depending on the nature of the application, there might be additional artifact types involved.

While bundles already provide a good way to define rather small, coherent modules, there is often a need to distribute or provision a set of such bundles together with some configuration. OSGi Deployment Admin and OSGi subsystems are two ways of trying to solve this issue. The feature model of Apache Karaf and the provisioning model of Apache Sling are two other approaches.

The goals of this proposal are:

* Defining a common mechanism to describe such features.
* Describe a common algorithm to combine features to either create higher level features or an OSGi applications.

# Requirements

## Model Requirements

The feature model should meet the following requirements:

* SFM010 - The feature model should be described through a text format which is easily consumable by both humans and machines, that can be edited with common editors and support text-based diff operations.
* SFM020 - A feature must be describable through a single file.
* SFM030 - Multiple features must be described in multiple files.
* SFM040 - The feature model language must support comments.
* SFM050 - The feature model may support more than one text-based definition language where the language used can be easily inferred, for example from the file extension.
* SFM060 - The feature model should provide support for long and multi-line values without creating files that become hard to handle.
* SFM070 - A feature model must have a unique identifier.
* SFM080 - A feature model must have a version.
* SFM090 - A feature model must be referenceable through Apache Maven coordinates.
* SFM100 - It must be possible to specify the bundles belonging to the feature, including version.
* SFM110 - It must be possible to specify the bundles in a feature in terms of Apache Maven coordinates.
* SFM120 - The feature model must allow the specification of the order in which the bundles inside the feature are started. This should be relative to when the feature itself is started.
* SFM130 - It must be possible to define whether a bundle is mandatory or optional.
* SFM140 - It must be possible to associate any additional metadata like a hash with a bundle.
* SFM150 - It must be possible to specify the OSGi configurations for a feature.
* SFM160 - Both normal OSGi configurations as well as factory configurations must be supported. The feature model must support all data types supported by the OSGi Configuration Admin specification.
* SFM170 - The OSGi configuration resource format as defined in the OSGi Configurator Specification must be supported.
* SFM180 - It must be possible to associate an OSGi configuration with a bundle within a feature. If the bundle is not resolved at runtime then the associated configuration also does not get installed.
* SFM190 - It must be possible to define framework properties.
* SFM200 - The feature model must be extensible to allow other artifacts than bundles.
* SFM210 - It must be possible to specify the artifacts in a feature in terms of Apache Maven coordinates.
* SFM220 - It must be possible to associate any additional metadata like a hash with an artifact.
* SFM230 - It must be possible to define whether an artifact is mandatory or optional.
* SFM240 - The feature model must be extensible to allow other/additional content.
* SFM250 - It must be possible to mark the additional content as optional.
* SFM260 - A feature must be able to specify additional requirements and capabilities that extend the requirements and capabilities from the contained artifacts.
* SFM270 - A feature must be able to extend other features.
* SFM280 - A feature must be able to depend on other features through the requirements/capabilities model based on the feature contents. The feature model must be able to deal with circular dependencies. However, there must be now way of explicitly requiring a feature from another feature.
* SFM290 - The feature model must describe how several features are aggregated to build a higher level feature
* SFM300 - The feature model must describe how several features are combined to build an application.
* SFM310 - When features are aggregated, either to create a higher level feature or an application, and a bundle/artifact is encountered with different versions, the feature model must be capable of only using the bundle/artifact with the highest version number.
* SFM320 - When features are aggregated, either to create a higher level feature or an application, and a bundle/artifact is encountered with different versions, the feature model must be capable of including both versions side-by-side.
* SFM330 - When features are aggregated, either to create a higher level feature or an application, the resulting feature or application must be minimal meaning it must not contain additional or unneeded artifacts.
* SFM340 - The feature model must support controlling of the exported API as described in https://github.com/apache/sling-whiteboard/blob/master/featuremodel/apicontroller.md
* SFM350 - The feature model must support explicit ordering of features. It must be possible to provide this ordering in separate file.
* SFM360 - The feature model must support variables to be used throughout the model, avoiding the need to repeat the same value several times.
* SFM370 - When features are aggregated, the ordering of the processing of those features needs to be predictable and stable.

## Tooling
The following requirements relate to tooling around the Feature Model.


* SFT010 - Tooling must support runtime launching with one or more features.
* SFT020 - Tooling must be able to compute the effective requirements of a feature by inspecting the feature's content and combining this with requirements specified on the feature itself.
* SFT030 - Tooling must be able to compute the capabilities of a feature by inspecting the feature's content and directly specified capabilities.
* SFT040 - Given one or more root features, the feature model must be able to compute the start order of all resulting features so that dependencies of features are started first.
  * Comment(CZ) : Is this tooling or feature model?
  * Comment(DB) : It could be either, but if there is support for explicit ordering of features (SFM350) then a tool could compute the ordering beforehand and place it in this file.
* SFT050 - Tooling must be able to find all features that provide the capabilities required by a given feature, from a set of available features.
* SFT060 - It should be possible to dynamically install and uninstall features at runtime.
* SFT070 - The feature model must support substitution of variable values at launch time.

### Containers
These requirements

* SFC010 - The feature model must support operation in a container environment such as Docker.
* SFC020 - The feature model must support micro-services oriented target runtimes where a single micro service runs in its own container containing only the binaries required for that microservice.
* SFC030 - It must be possible to add new features by placing additional files in a container's file system.
* SFC040 - It must be possible to alter existing features by placing additional files in the file system. For example to uninstall or update a bundle provided by an existing feature or to alter a configuration set by an existing feature.
* SFC050 - The feature model must enable the creation of container (Docker) images for a specified set of features.

### Sling Specific

* SFS010 - The feature model must support JCR Repository Initialization via the _repoinit_ language. See SFM240
* SFS020 - The feature model must support features which contains repository content packages. See SFM200
* SFS030 - The feature model should support all functionality previously provided by the Sling provisioning model.
* SFS040 - A (Maven) tool must be provided that can create a launchable Sling Starter application from the feature model.

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

Notes for users of Apache Sling's provisioning model:

* No support for run modes in the model - run modes can be modeled through separate features

# Feature Identity

A feature has a unique id. Maven coordinates (https://maven.apache.org/pom.html#Maven_Coordinates) provide a well defined and accepted way of uniquely defining such an id. The coordinates include at least a group id, an artifact id, a version and a type/packaging. A classifier is optional.

While group id, artifact id, version and the optional classifier can be freely choosen for a feature, the type/packaging is defined as "osgifeature".

TBD: Is "osgifeature" a good type?

# Maven Coordinates

Maven coordinates are used to define the feature id and to refer to artifacts contained in the feature, e.g. bundles, content packages or other features. There are two supported ways to write down such a coordinate:

* Using a colon as a separator for the parts: groupId:artifactId[:type[:classifier]]:version as defined in https://maven.apache.org/pom.html#Maven_Coordinates
* Using a mvn URL: 'mvn:' group-id '/' artifact-id [ '/' [version] [ '/' [type] [ '/' classifier ] ] ] ]

In some cases only the coordinates are specified as a string in one of the above mentioned formats. In other cases, the artifact is described through a JSON object. In that case, the *id* property holds the coordinates in one of the formats.

# Requirements and Capabilities vs Dependencies

In order to avoid a concept like "Require-Bundle" a feature does not explicitly declare dependencies to other features. These are declared by the required capabilities, either explicit or implicit. The implicit requirements are calculated by inspecting the contained bundles (and potentially other artifacts like content packages ).

Once a feature is processed by tooling, the tooling might create a full list of requirements and capabilities and add this information in a special section to the final feature. This information can be used by tooling to validate an instance (see below) and avoids rescanning the binary artifacts. However this "cached" information is optional and tooling must work without it (which means it needs access to the binaries in that case). TBD the name and format of this information.

# Feature Header

The JSON feature object has the following properties:

* *id* : The feature id as described above. Defining the type is optional, if it is not defined, it defaults to "osgifeature".
* *title* : Optional title for the feature.
* *description* : Optional long text description of the feature.
* *vendor* : Optional vendor information.
* *license* : Optional license information

# Bundles

The JSON feature object might contain a *bundles* property holding a JSON array with the bundles contained in this feature. The values in this array can either be a string holding the coordinates for the bundle or a JSON object if additional properties need to be specified. The JSON object supports the following properties:

* *id* : The id of the bundle (maven coordinates). This property is required.
* *start-order* : The value is an integer greater or equals to 1. This specifies the start order of the bundle. The start-order specifies the start order of the bundle in relation to other bundles in the feature. Bundles are started in ascending order and stopped in descending order according to the start-order directive values. Bundles with the same start-order value may be started and stopped in any order in relation to each other. There is no default value for start-order. If the start order
is not specified then the bundle is started in any order.
* *resolution* : The value is either "mandatory" or "optional". A mandatory bundle needs to be satisfied; an optional bundle does not need to be satisfied. The default value is mandatory.

TBD: The implementation currently uses the map based configuration. It needs to change.

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


# Appendix A : Provisioning Applications

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

# Appendix B : Apache Sling's Provisioning Model

The documentation for Apache Sling's provisioning model can be found here: https://sling.apache.org/documentation/development/slingstart.html

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
