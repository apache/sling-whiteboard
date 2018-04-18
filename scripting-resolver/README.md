Apache Sling Scripting Reloaded
====

## Overview

Apache Sling embraced David's Model [1] when it was designed, therefore everything is content, including rendering scripts. While this mechanism makes script deployment an easy task, it leads to several disadvantages:

* scripts are directly accessible by platform users, if they have read permissions for the search paths, although scripts should have a developer-only audience
* scripts can be overlaid freely through the search path override or through the special sling:resourceSuperType property, complicating a developer's understanding of the script resolution mechanism
* changing a previously available script in an application can pose backwards compatibility and upgrade issues that the developers have to handle through various workarounds (e.g. path-based versioning)

The only way to overcome the stated disadvantages is to redesign the way scripting works:

* component scripts should be deployed through bundles, exactly like Java code;
* component scripts should should be organised into semantically versioned packages, with clear definitions for what constitutes:
    * a micro change
    * a minor change
    * a major change
* a bundle providing scripts should explicitly declare the feature(s) (resource types), by making use of the`Provide-Capability` [2] header;
* a bundle providing scripts that extend an existing feature (resource type) should declare this intention explicitly:
    * by employing the `Require-Capability` [2] header, to reference the feature the bundle extends
    * a `Provide-Capability` header should describe the new feature (resource type), obtained by extending

Advantages of the new design:

* the Require / Provide explicit mechanism removes the need for search paths and also for the sling:resourceSuperType mechanism, transforming script resolution essentially into a Map built at deploy time;
* backwards compatibility will be handled the same way we do for Java APIs, allowing for smoother upgrades and delivery of new features
* such a model could be described through Apache Sling's Feature Model [3], allowing an application to be built using a mix & match technique for collecting the components it requires for rendering.


## Further reading
For a detailed view of how this project works please check the [`docs`](docs) folder.

## References

[1] - https://wiki.apache.org/jackrabbit/DavidsModel  
[2] - https://osgi.org/download/r6/osgi.core-6.0.0.pdf, Page 41, section 3.3.3 "Bundle Capabilities"  
[3] - https://github.com/apache/sling-whiteboard/tree/master/featuremodel
