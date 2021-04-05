# Apache Sling Declarative Dynamic Resources

## Introduction

In Peregrine CMS each tenant had their own set of components that were just a redirection through
their Sling Resource Super Type to a component provided by Peregrine. This way the Tenant could maintain
their components but Peregrine could add new components w/o affecting or confusing the tenant.
In addition, if Peregrine wants to make new components available we face the issue of a read-only
JCR tree in /libs and /apps which would require a new deployment and restart of Peregrine.

## Declarative Dynamic Resource

Both of these issues can be solved by creating Synthetic Resources which appear in the desired target
resource but its actual content is provided from another resource.

The DDR is looking for dedicated resources which become the source (provider) folders. These folders must also
provide the path of the dynamic (virtual) target folder. All resources in the source folder will create
a dynamic resource with the same name in the target folder. The target folder must exist and if
a resource with the same name already exists in the target folder then the DDR is not visible / accessible.

To limit or prevent abuse (Like script injection) the **DD Resource Manager** can be configured to filter
out undesired resources based on their properties. For example scripts can be filtered out with a filter
on **jcr:primaryType** of **nt:resource**.

**Note**: any dynamic resource will have a property called **sling:ddrActive** with value **true** that
marks this resource as a DDR.

The Declarative Dynamic Resource (DDR) is composed of these components:

* Declarative Dynamic Resource Manager: listens to DDRs becoming available and then creates the Synthetic Resources needed
* Declarative Dynamic Resource Provider: a Resource Provider for any DDR source properly configured
* Declarative Dynamic Resource: a Synthetic (virtual) Resource and takes its properties from a source (provider)
* Declarative Dynamic Resource Listener: an interface that informs the implementor about newly available provider folders

The DDR is using its own Service User **ddr-serviceuser** to read and handle DDRs through a Service Resource Resolver.

### Source Folder

Any source folder needs to set the **jcr:primaryType** to **sling:DDR** and set the property
**sling:ddrTarget** to the path of the target folder of its DDR. That path needs to be absolute and
point to an existing resource. These target folder can contain other resources (JCR or DDR) but duplicates
are not visible.

### DDR Filtering

To prevent users from installation undesired nodes as DDRs the DDR Manager can be configured to filter
nodes by their properties. The filter entries are in the format: `<property name>=<property value>` and any
filter that does not provide an equals sign is ignored. These are the configuration properties:

* **allowed.ddr.filter**: any node without a matching property is removed 
* **prohibited.ddr.filter**: any node with a matching property is removed

Note that **allowed filters** allow any nodes that have at least one matching (all nodes if no filter is provided)
and that **prohibited filters** removes any nodes that have at least one matching (none if no filter is provided).

For example if you want to limit DDRs to folders (sling:Folder and nt:folder) except the ones with a title
/ description (jcr:title, jcr:description) of 'Test' then you can do:

* **allowed.ddr.filter**: [ "jcr:primaryType=sling:Folder", "jcr:primaryType=nt:folder" ]
* **prohibited.ddr.filter**: [ "jcr:title=Test", "jcr:description=Test" ]

With that it is possible to prevent the users to add a script as DDR by prohibiting nt:file and nt:resources:

* **prohibited.ddr.filter**: [ "jcr:primaryType=nt:file", "jcr:primaryType=nt:resource" ]

## Example

Configuration:
```
/conf
  /test
    /settings
      /dynamic
        - jcr:primaryType = sling:DDR
        - sling:ddrTarget = /apps/test/components
        /button1
          - jcr:primaryType = sling:Folder
          - sling:resourceSuperType = sling/components/button
```
Apps:
```
/apps
  /sling
    /components/
      /button
        /button.html
          ...
  /test
    /components
      /text
        ...
```
After installing DDR:
```
/apps
  /sling
    ... (same as above)
  /test
    /components
      /button1
        - jcr:primaryType = sling:Folder
        - sling:resourceSuperType = sling/components/button
        - sling:ddrActive = true
      /text
        ... (same as above)
/conf
  ... (same as above)
```
## Samples

The DDR project comes with two samples to illustrate how it is used:

* **sample.installation**: this sample is installed during the installation of DDR project and shows how
  the DDRs are handled when the configuration resources are already installed and so the DDR Manager
  picks them up during activation.
* **sample.after**: this sample is installation manually after the installation of the DDR project. This scenario
  happens when a new configuration is installed after the DDR Manager is activated hence the DDR Manager is
  receiving a **Node Added** event which then triggers the handling of the DDR source folder.
  

Andreas Schaefer, 4/3/2021