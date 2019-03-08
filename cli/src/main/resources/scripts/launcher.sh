#!/bin/sh
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

# TODO - contribute '-q' flag to launcher OR allow passthrough of org.slf4j.simpleLogger system properties


# funky syntax needed to properly preserve arguments with whitespace
ARGS_PROP="exec.args=$@"

# Use exec to become pid 1, see https://docs.docker.com/develop/develop-images/dockerfile_best-practices/
exec /usr/bin/java \
	 -Dorg.slf4j.simpleLogger.logFile=/dev/null \
	 -Dlogback.configurationFile=file:/usr/share/sling-cli/conf/logback-default.xml \
	 -jar /usr/share/sling-cli/launcher/org.apache.sling.feature.launcher.jar \
	 -f /usr/share/sling-cli/sling-cli.feature \
	 -c /usr/share/sling-cli/artifacts \
	 -D "$ARGS_PROP" \
	 -V "asf.username=${ASF_USERNAME}" \
	 -V "asf.password=${ASF_PASSWORD}"