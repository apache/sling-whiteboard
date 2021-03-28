# Declarative Dynamic Resource Sample

This is a sample project to showcase the use of DDRs in Sling. It requires a Sling 12
instance to be installed on.

## Design

These are the components of the Sample:

* **/apps/ddr-dynamic/components**: the DDR target folder which is empty
* **/apps/ddr-sample/config**: runmode configuration for this Sample
* **/apps/ddr-static**: the base component **button** and **text**
* **/conf/ddr-sample/settings/dynamic**: the source DDR components that points to the DDR target of **/apps/ddr-dynamic/components and contains an updated version of Button and Text
* **/content/ddr-sample**: content that show the usage of enhanced Button and Text

## What Happens

After the DDR Sample is installed the DDR Manager will discover that */conf/ddr-sample/settings/dynamic*
is a DDR source (by its primary type) and then read out the DDR target path. Then it will register that
folder as DDR Source / Target pair with the DDR Provider. Whenever now a user requests a resource from that
folder the DDR Provider will look it up and if:

* it finds an existing resource in that folder -> return that resource
* it finds a resource in the DDR source with that name -> return a Synthetic Resource that contains the properties of the DDR source with the same name

## Review

After the installation open Sling in a browser and then go to **composum** browser.
Here go to **/apps/ddr-dynamic** and you will find two child nodes **button1** and **text1**.
These resources are fully dynamic mean that they would go away if the DDR core is disabled.
Try this by going to the System Console **Bundles** and stop the **org.apache.sling.ddr.core** bundle.
Refershing the **/apps/ddr-dynamic** folder in composum will not display any child resources. Restart
the bundle now again and make sure the child resources are there again.
Now we want to see these components but open the content on **/ddr-sample/button.html**. This will
show *Hello Button: * followed by the path of the resource.

Andreas Schaefer: 3/27/2021