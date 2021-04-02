# Apache Sling Declarative Dynamic Resources

## Introduction

In Peregrine CMS we were facing the fact that each tenant had their own set of components that were
just a redirection through their Sling Resource Super Type to a component provided by Peregrine. This
was the Tenant could maintain their components but Peregrine could add new components w/o affecting
or confusing the tenant.
In addition, if Peregrine wants to make new components available we face the issue of a read-only
JCR tree in /libs and /apps which would require a new deployment and restart of Peregrine.

## Declarative Dynamic Resource

Both of these issues can be solved by creating Synthetic Resources that appear in the desired target
resource but its actual content is provided from another node.
The Declarative Dynamic Resource (DDR) is composed of these components:

* Declarative Dynamic Resource Manager: listens to DDRs becoming available and then creates the Synthetic Resources needed
* Declarative Dynamic Resource Provider: a Resource Provider for any DDR source properly configured
* Declarative Dynamic Resource: a Synthetic Resource that lives in a given node and points to its source for its properties
* Declarative Dynamic Resource Listener: an interface that informs the implementor about newly created DDRs

The DDR is using its own Service User **ddr-serviceuser** that is used to read and handle DDRs.

Any source folder needs to set the **jcr:primaryType** to **sling:DDR** and set the property
**sling:ddrTarget** to the path of the target folder of its DDR. That path needs to be absolute and
point to an existing resource. Keep in mind that this path points to the parent of a DDR as each node
inside the source is creating a DDR with the path of the target as parent and the source name as
name of the DDR. Keep in mind that **no resource with that name** can existing in the target folder
otherwise it will be shadowed by the existing resource.

**Note**: any dynamic resource will have a property called **sling:ddrActive** with value **true** that
marks this resource as a DDR.

Andreas Schaefer, 3/27/2021