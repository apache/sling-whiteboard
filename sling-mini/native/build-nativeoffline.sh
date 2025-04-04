#!/bin/sh

pushd target

ATOMOS_CLASSPATH=""
ATOMOS_INIT=""

native-image --verbose \
-cp "artifacts/org/apache/sling/org.apache.sling.feature.launcher.atomos/0.0.1-SNAPSHOT/org.apache.sling.feature.launcher.atomos-0.0.1-SNAPSHOT.jar:\
artifacts/org/apache/sling/org.apache.sling.feature.launcher/1.2.4/org.apache.sling.feature.launcher-1.2.4.jar:\
artifacts/org/apache/felix/org.apache.felix.atomos/1.0.1-SNAPSHOT/org.apache.felix.atomos-1.0.1-SNAPSHOT.jar:\
artifacts/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar:\
artifacts/org/slf4j/slf4j-simple/1.7.25/slf4j-simple-1.7.25.jar:\
artifacts/org/apache/sling/org.apache.sling.feature/1.3.0/org.apache.sling.feature-1.3.0.jar:\
artifacts/org/apache/felix/org.apache.felix.cm.json/1.0.6/org.apache.felix.cm.json-1.0.6.jar:\
artifacts/commons-cli/commons-cli/1.4/commons-cli-1.4.jar:\
artifacts/org/apache/felix/org.apache.felix.framework/7.0.5/org.apache.felix.framework-7.0.5.jar:\
artifacts/org/apache/sling/org.apache.sling.commons.johnzon/1.2.16/org.apache.sling.commons.johnzon-1.2.16.jar:\
artifacts/org/apache/commons/commons-text/1.10.0/commons-text-1.10.0.jar:\
atomos-config/app.substrate.jar" \
org.apache.sling.feature.launcher.impl.Main \
-o sling_native -O3  "-march=native" "--gc=epsilon" --no-fallback \
"$@"

popd

# --pgo-instrument / --pgo
#
