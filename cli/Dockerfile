# ----------------------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one or more contributor license
# agreements. See the NOTICE file distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file to you under the Apache License,
# Version 2.0 (the "License"); you may not use this file except in compliance with the
# License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software distributed under the
# License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
# either express or implied. See the License for the specific language governing permissions
# and limitations under the License.
# ----------------------------------------------------------------------------------------

FROM openjdk:8-jre-alpine
MAINTAINER dev@sling.apache.org
# escaping required to properly handle arguments with spaces
ENTRYPOINT ["/usr/share/sling-cli/bin/launcher.sh"]

# Add feature launcher
ADD target/lib /usr/share/sling-cli/launcher
# Add launcher script
ADD target/classes/scripts /usr/share/sling-cli/bin
# workaround for MRESOURCES-236
RUN chmod a+x /usr/share/sling-cli/bin/*
# Add config files
ADD target/classes/conf /usr/share/sling-cli/conf
# Add all bundles
ADD target/artifacts /usr/share/sling-cli/artifacts
# Add the service itself
ARG FEATURE_FILE
ADD ${FEATURE_FILE} /usr/share/sling-cli/sling-cli.feature