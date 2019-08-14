[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-slingfeature-maven-plugin/master)](https://builds.apache.org/job/Sling/job/sling-slingfeature-maven-plugin/job/master) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-slingfeature-maven-plugin/job/master.svg)](https://builds.apache.org/job/Sling/job/sling-slingfeature-maven-plugin/job/master/test_results_analyzer/) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/slingfeature-maven-plugin/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22slingfeature-maven-plugin%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/slingfeature-maven-plugin.svg)](https://www.javadoc.io/doc/org.apache.sling/slingfeature-maven-plugin) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Feature Converter Maven Plugin

This module is part of the [Apache Sling](https://sling.apache.org) project.

This plugin provides the means to convert both Content Packages as well as
Provisioning Models into Feature Models through a POM file.

# Introduction

This plugin is intended to convert:
* Content Packages
* Provisioning Models
into Feature Models so that it can be used by the Sling Feature Maven
Plugin to assemble and run.
 
This plugin is a wrapper for the Sling Feature Content Package Converter
**sling-org-apache-sling-feature-cpconverter** and the Sling Feature Model
Converter **sling-org-apache-sling-feature-modelconveter** to convert
source files into Feature Models inside a POM.

This plugin is normally used to convert Sling into its Feature Model
version and to convert Content Packages into Feature Models so that they
can be handled by the Sling Feature Maven Plugin and eventually launched.

## Supported goals

These Goals are support in this plugin:

* **convert-cp**: Converts Content Packages into Feature Model files
and its converted ZIP files
* **convert-pom**: Converts Provisioning Models into Feature Models

See the Site documentation for further info.