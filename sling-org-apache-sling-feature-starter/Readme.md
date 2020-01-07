# Sling Feature Starter

This project is the Feature Model based version of the **sling-org-apache-sling-starter**
module and creates an executable JAR file for now.
It is also a test case for the Slingstart Feature Maven Plugin.

## Build

This plugin depends on the **Sling Start Maven Plugin** (also in the Sling
Whiteboard) which is for now extracted in its own profile **launch**:

1. Go to **sling-slingstart-feature-maven-plugin** module in Sling Whiteboard
2. Build with: `mvn clean install`
3. Go back to **sling-org-apache-sling-feature-starter**
4. Build and Launch it with: `mvn clean install -P launch`
5. Sling will come up and is accessible on the temporary port but the build
will not end

## Issues

This module must be able to launch Sling in a demon (background) thread
so that the starter can exit. The question is if that is added here or to
the Feature Launcher module.
