#!/bin/sh

pushd target

source ./atomos-config/atomos_init.sh

native-image --verbose \
-cp "artifacts/org/apache/sling/org.apache.sling.feature.launcher.atomos/0.0.1-SNAPSHOT/org.apache.sling.feature.launcher.atomos-0.0.1-SNAPSHOT.jar:\
artifacts/org/apache/sling/org.apache.sling.feature.launcher/1.2.4/org.apache.sling.feature.launcher-1.2.4.jar:\
artifacts/org/apache/felix/org.apache.felix.atomos/1.0.1-SNAPSHOT/org.apache.felix.atomos-1.0.1-SNAPSHOT.jar:\
artifacts/org/slf4j/slf4j-simple/1.7.25/slf4j-simple-1.7.25.jar:\
artifacts/org/apache/sling/org.apache.sling.feature/1.3.0/org.apache.sling.feature-1.3.0.jar:\
artifacts/org/apache/felix/org.apache.felix.cm.json/1.0.6/org.apache.felix.cm.json-1.0.6.jar:\
artifacts/commons-cli/commons-cli/1.4/commons-cli-1.4.jar:\
$ATOMOS_CLASSPATH" \
org.apache.sling.feature.launcher.atomos.AtomosLaucherMain \
--no-fallback --enable-https --enable-http \
"$ATOMOS_INIT" \
"-H:ResourceConfigurationFiles=atomos-config/resource-config.json" \
"-H:ReflectionConfigurationFiles=atomos-config/reflect-config.json" \
"-H:DynamicProxyConfigurationFiles=atomos-config/proxy-config.json" \
"-H:JNIConfigurationFiles=atomos-config/jni-config.json" \
"-H:SerializationConfigurationFiles=atomos-config/serialization-config.json" \
"-o aem_native" "-Ob" \
"$@"

popd
