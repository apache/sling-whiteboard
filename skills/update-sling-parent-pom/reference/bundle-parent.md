## Migrate to new Sling Parent pom location

Starting with version 35 the coordinates of the Sling parent pom have changed to org.apache.sling:sling-bundle-parent for OSGi bundles.

Projects that are not OSGi bundles can continue using the old coordinates.

## pom.xml changes for bundle projects

Remove the maven-bundle-plugin and use the bnd-maven-plugin instead.

