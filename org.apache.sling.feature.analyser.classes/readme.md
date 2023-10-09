[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/test/?width=800&height=600)&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-analyser&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-analyser)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-analyser&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-analyser)&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.feature.analyser.svg)](https://www.javadoc.io/doc/org.apache.sling/org-apache-sling-feature-analyser)&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.feature.analyser/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.feature.analyser%22)&#32;[![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/group/feature.md) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Feature Model Analyser For Classes

This module contain analyser tasks which analyse classes contained in features' OSGi bundles.
For further information about refer to [Sling Feature Model Analyser](https://github.com/apache/sling-org-apache-sling-feature-analyser)


# Analyser Tasks

Below is a list of built-in analysers.

## `prevent-provider-type-impls`

This analyser makes sure that no class implements or extends any type marked as provider with annotation [`org.osgi.annotation.versioning.ProviderType`](https://docs.osgi.org/javadoc/osgi.annotation/8.0.0/org/osgi/annotation/versioning/ProviderType.html).

