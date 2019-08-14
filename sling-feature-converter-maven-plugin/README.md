[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=Sling/sling-slingfeature-maven-plugin/master)](https://builds.apache.org/job/Sling/job/sling-slingfeature-maven-plugin/job/master) [![Test Status](https://img.shields.io/jenkins/t/https/builds.apache.org/job/Sling/job/sling-slingfeature-maven-plugin/job/master.svg)](https://builds.apache.org/job/Sling/job/sling-slingfeature-maven-plugin/job/master/test_results_analyzer/) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/slingfeature-maven-plugin/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22slingfeature-maven-plugin%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/slingfeature-maven-plugin.svg)](https://www.javadoc.io/doc/org.apache.sling/slingfeature-maven-plugin) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Apache Sling Content Package to Feature Model Plugin

This module is part of the [Apache Sling](https://sling.apache.org) project.

Maven Plugin for Content Package to Feature Model Conversion:

# Introduction

This plugin is intended for a content package to also provide a Feature Model
and its converted package / bundle. This plugin will only work on Content
Package ZIP files artifacts.

This plugin is a wrapper for the Sling Feature Content Package Converter
**org.apache.sling.feature.cpconverter** over a content package the
POM defines.

The idea is to convert a Content Package at built time into a Feature
Module so that it can be used both a regular Content Package in a
traditional Sling Instance (Lauchpad) or in a Sling Instance in a Feature
Model.
The plugin will do the conversion and so a Content Package cna be used
in a Feature Model w/o any changes in the Content Package.

## Supported goals

This plugin has only one goal called **convert-cp** which takes a few
parameters:

* **strictValidation**:
    if set to true this will force the plugin to do a strict
    conversion. Checkout the Content Package 2 Feature
    Model Converter tool. Default: **false**
* **mergeConfigurations**:
    if set to true if will merge configurations with
    the same PID. Not sure if that makes sense in
    this plugin. Default: **false**
* **bundleStartOrder**:
    The bundle start order of the generated Feature
    Model. Default **20**
* **artifactIdOverride**:
    An FM Artifact Id that overrides the one that
    is set by default. **Atttention**: to avoid the
    variable substitution for **${}** please add
    a zero-width whitespace (&#8203;) in between.
* **featureModelsOutputDirectory**:
    Output folder of the Feature Model. Default
    **target/cp-conversion**
* **convertedContentPackageOutputDirectory**:
    Output folder folder of the converted Content Package. Default
    **target**
* **installConvertedContentPackage**:
    Install the Converted Content Package into the local Maven
    Repository so that it can be used by the Feature Model without
    a manual copy. Default: **true**
* **systemProperties**:
    List of strings that represents System Properties like the Java
    **-D** option

In order for the plugin to work the Content Package must be already
created so this plugin must be started in a lifecycle phase after
**package**.
