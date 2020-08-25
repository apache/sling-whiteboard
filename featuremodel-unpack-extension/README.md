# Apache Sling Featuremodel - Unpack Exension

This module provides support for a generic "unzip" extension for the feature model. It allows you to have feature model artifact extensions unzipped to the file system.

The jar can be used in the following ways:

## Feature model launcher extension

When added to the classpath of the feature launcher it will unzip extensions according to a framework property (see below).

## OSGi installer extension

When installed as a bundle into sling, it will act as a extension to the OSGi installer and handle zip files that get installed as well as extensions in feature models that get installed (both, again, according to a framework property).


## Converter

When invoked on the commandline, there is a converter main class that can be used to wrap a list of urls into a feature with an extension.

It looks like this:

```
java -cp ${HOME}/.m2/repository/org/apache/sling/org.apache.sling.commons.johnzon/1.2.2/org.apache.sling.commons.johnzon-1.2.2.jar:${HOME}/.m2/repository/org/apache/sling/org.apache.sling.feature/1.2.6/org.apache.sling.feature-1.2.6.jar:target/org.apache.sling.feature.extension.unpack-0.1.0-SNAPSHOT.jar \
 org.apache.sling.feature.extension.unpack.impl.converter.Converter \
 <mvn-id-of-resulting-feature> \
 <name-of-extension-in-feature> \
 <path-to-resulting-feature-file> \
 <path-to-mvn-repository-to-store-artifacts> \
 key=<optional-key-for-manifest> \
 value=<optional-valure-for-manifest>\
 <space separated list of urls>

```


## Configuration

The framework property ```org.apache.sling.feature.unpack.extensions``` can be used to give an OSGi header clause configuring which extensions to unzip, to which dir, and with what required manifest header.

It looks like this:

```-Dorg.apache.sling.feature.unpack.extensions=<extension-name>;dir:=<path-to-dir-on-disc>;default:=<[true|false]>;key:=<optional-key-in-manifest>;value:=<optional-value-for-key-in-manifest>;index:=<path-header-in-manifest><[,<clause>]*>```





