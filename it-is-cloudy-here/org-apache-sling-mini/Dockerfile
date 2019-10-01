# -----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# -----------------------------------------------------------------------------
FROM azul/zulu-openjdk-alpine:11 as builder
MAINTAINER dev@sling.apache.org
ARG JAVA_DESKTOP
RUN MODULES="java.base,java.compiler,java.instrument,java.logging,java.management,java.naming,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.unsupported"; \
    if [ "$JAVA_DESKTOP" == "true" ]; then \
        MODULES="java.desktop,${MODULES}"; \
    fi; \
    jlink --add-modules ${MODULES} --output /opt/jre --strip-debug --compress=2 --no-header-files --no-man-pages;

FROM alpine
EXPOSE 8080
WORKDIR /opt/run
RUN addgroup -S sling && adduser -S sling -G sling && \
    chown -R sling:sling /opt/run

COPY --from=builder /opt/jre /opt/jre
COPY target/artifacts /opt/app/artifacts
COPY target/docker-run/org.apache.sling.feature.launcher.jar /opt/run
COPY target/docker-run/feature.json /opt/run

USER sling
CMD /opt/jre/bin/java \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED \
    --add-opens=java.base/java.net=ALL-UNNAMED \
    --add-opens=java.base/java.security=ALL-UNNAMED \
    -jar org.apache.sling.feature.launcher.jar \
    -u file:/opt/app/artifacts \
    -f feature.json
