# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
CLIENT_SECRET := $(shell cat src/test/resources/keycloak-import/sling.json | jq --raw-output '.clients[] | select (.clientId == "oidc-test") | .secret')
KEYCLOAK_PORT := 8081

keycloak-run-import:
	docker run --name=keycloak-sling --rm --volume $(CURDIR)/src/test/resources/keycloak-import:/opt/keycloak/data/import -p $(KEYCLOAK_PORT):8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.3 start-dev --import-realm

sling-run:
	mvn feature-launcher:start feature-launcher:stop -Dfeature-launcher.waitForInput

sling-create-config:
	curl --verbose -u admin:admin -X POST -d "apply=true" -d "propertylist=name,baseUrl,clientId,clientSecret,scopes" \
    		-d "name=keycloak-dev" \
		-d "baseUrl=http://localhost:$(KEYCLOAK_PORT)/realms/sling" \
    		-d "clientId=oidc-test"\
    		-d "clientSecret=$(CLIENT_SECRET)" \
    		-d "scopes=openid" \
    		-d "factoryPid=org.apache.sling.auth.oauth_client.impl.OidcConnectionImpl" \
    		http://localhost:8080/system/console/configMgr/org.apache.sling.auth.oauth_client.impl.OidcConnectionImpl~keycloak-dev
