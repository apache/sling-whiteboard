[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)


# Apache Sling - Commons Log JSON

Generate JSON logs to the console using logback with support for configuring logger levels via Sling Commons Logging configurations

## Use

Note bundle depends on the following bundles being installed at the same start level:

 -  ch.qos.logback:logback-core:1.2.10
 -  ch.qos.logback:logback-classic:1.2.10
 -  net.logstash.logback:logstash-logback-encoder:7.0.1
 -  org.apache.felix:org.apache.felix.log:1.2.6
 -  org.apache.felix:org.apache.felix.logback:1.0.2
 -  com.fasterxml.jackson.core:jackson-annotations:${jackson-version}
 -  com.fasterxml.jackson.core:jackson-core:${jackson-version}
 -  com.fasterxml.jackson.core:jackson-databind:${jackson-version}
 -  org.apache.sling:org.apache.sling.commons.log.logback.configurator:1.0.0-SNAPSHOT
