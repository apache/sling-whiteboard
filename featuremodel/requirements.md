# An Introduction to the OSGi Feature Model

Typical OSGi applications are assembled out of bundles and configured through both, OSGi configurations and framework properties (though these are less frequently used than OSGi configurations). Depending on the nature of the application, there might be additional artifact types involved.

While bundles already provide a good way to define rather small, coherent modules, there is often a need to distribute or provision a set of such bundles together with some configuration. OSGi Deployment Admin and OSGi subsystems are two ways of trying to solve this issue. The feature model of Apache Karaf and the provisioning model of Apache Sling are two other approaches.

The established common term for such higher level modules is feature. The goals of this proposal are:

* Defining a common mechanism to describe such features.
* Describe a common algorithm to combine features to either create higher level features or an OSGi applications.

The model is a general purpose feature model and in no way tied to Apache Sling.

# Requirements

## Model Requirements

The feature model is about describing a feature, aggregating features to either build higher level features or an application. The model should meet the following requirements:

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
* SFM290 - The feature model must describe how several features are aggregated to build a higher level feature. This description must include all parts of the feature model (bundles, configurations, framework properties etc.). The description should be general for extension, which means it should describe how extensions are aggregated without requiring the model implementation to know the type of extension.
* SFM300 - The feature model must describe how several features are combined to build an application. This description must include all parts of the feature model (bundles, configurations, framework properties etc.). The description should be general for extension, which means it should describe how extensions are aggregated without requiring the model implementation to know the type of extension.
* SFM310 - When features are aggregated, either to create a higher level feature or an application, and a bundle/artifact is encountered with different versions, the feature model must be capable of only using the bundle/artifact with the highest version number. The detection is based on the artifact/bundle id, not the bundle symbolic name.
* SFM320 - When features are aggregated, either to create a higher level feature or an application, and a bundle/artifact is encountered with different versions, the feature model must be capable of including both versions side-by-side. The detection is based on the artifact/bundle id, not the bundle symbolic name.
* SFM330 - When features are aggregated, either to create a higher level feature or an application, the resulting feature or application must be minimal meaning it must not contain additional or unneeded artifacts.
* SFM340 - The feature model must support controlling of the exported API as described in https://github.com/apache/sling-whiteboard/blob/master/featuremodel/apicontroller.md
* SFM350 - The feature model must calculate the startup order of bundles for an aggregated application respecting the dependencies between features and their contents.
* SFM360 - The feature model must support variables to be used throughout the model, avoiding the need to repeat the same value several times.
* SFM370 - When features are aggregated, the ordering of the processing of those features needs to be predictable and stable.
* SFM380 - The feature model must support adding or overwriting requirements and capabilities of a contained bundle or artifact. This is in oder to correct invalid metadata or to add missing metadata of the artifact.
* SFM390 - The feature model must support adding or overwriting manifest headers for a bundle. For example to allow to change the bundle symbolic name or to add missing OSGi metadata to a plain jar file.
* SFM400 - The feature model must support a textual representation for an application aggregated out of features. The format should be as similar as possible to the feature format.
* SFM410 - It must be possible to specify the framework to launch an application as part of the application model.
* SFM420 - When features are aggregated to either a higher level feature or an application, the resulting feature or application must still contain the variables.
* SFM430 - The startup order of features and bundles must be part of the resulting aggregated application model.

## Analysis Requirements

* SFA010 - Tooling must be able to compute the effective requirements of a feature by inspecting the feature's content and combining this with requirements specified on the feature itself.
* SFA020 - Tooling must be able to compute the capabilities of a feature by inspecting the feature's content and directly specified capabilities.
* SFA030 - The feature model should support to store the results of SFA010 and SFA020 as port of the model, avoiding duplicate calculations.

## Resolving Requirements

* SFR010 - Tooling must be able to find all features that provide the capabilities required by a given feature, from a set of available features.

## Launching Requirements

* SFL010 - Tooling must support creating an application model out of one or more features.
* SFL020 - Tooling must support runtime launching of an application.
* SFL030 - Tooling must be able to introspect and potentially override the startup order of bundles for an application.
* SFL040 - Tooling must support substitution of variable values at launch time.
* SFL050 - When an application is started, the install and the startup order of bundles should be the same, ensuring that the bundles are shutdown in reverse order and started in the same order on next startup of the framework.

## Runtime Requirements

* SFD010 - It should be possible to dynamically install and uninstall features at runtime.

## Container Requirements

* SFC010 - The feature model must support operation in a container environment such as Docker.
* SFC020 - The feature model must support micro-services oriented target runtimes where a single micro service runs in its own container containing only the binaries required for that microservice.
* SFC030 - It must be possible to add new features by placing additional files in a container's file system.
* SFC040 - It must be possible to alter existing features by placing additional files in the file system. For example to uninstall or update a bundle provided by an existing feature or to alter a configuration set by an existing feature.
* SFC050 - The feature model must enable the creation of container (Docker) images for a specified set of features.

## Sling Specific Requirements

These requirements are important for Apache Sling in order to have a full replacement for the current provisioning model. Ideally, these requirements are already covered by the general requirements.

* SFS010 - The feature model must support JCR Repository Initialization via the _repoinit_ language. See SFM240
* SFS020 - The feature model must support features which contains repository content packages. See SFM200
* SFS030 - The feature model should support all functionality previously provided by the Sling provisioning model.
* SFS040 - A (Maven) tool must be provided that can create a launchable Sling Starter application from the feature model.
