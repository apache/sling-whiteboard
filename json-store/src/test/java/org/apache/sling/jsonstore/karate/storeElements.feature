# /*
# * Licensed to the Apache Software Foundation (ASF) under one
#  * or more contributor license agreements.  See the NOTICE file
#  * distributed with this work for additional information
#  * regarding copyright ownership.  The ASF licenses this file
#  * to you under the Apache License, Version 2.0 (the
#  * "License"); you may not use this file except in compliance
#  * with the License.  You may obtain a copy of the License at
#  *
#  *   http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing,
#  * software distributed under the License is distributed on an
#  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  * KIND, either express or implied.  See the License for the
#  * specific language governing permissions and limitations
#  * under the License.
#  */

# ------------------------------------------------------------------------
@content
Feature: Test elements storage
# ------------------------------------------------------------------------

# ------------------------------------------------------------------------
Background:
# ------------------------------------------------------------------------

* url baseURL

# Use admin credentials for all requests
* configure headers = call read('classpath:util/basic-auth-header.js')

# ------------------------------------------------------------------------
Scenario: Cleanup previous test content
# ------------------------------------------------------------------------
Given path 'content/sites'
When method DELETE
* match [204,404,405] contains responseStatus

# TODO this test fails with mvn clean install although it
# works with -Dexternal.test.server.port, not sure why
# # ------------------------------------------------------------------------
# Scenario: Attempt to store content before having a schema
# # ------------------------------------------------------------------------
# Given request read('/content/minimal-content.json')
# And path 'content/sites/example.com/branches/authoring/elements/somepath/minimal-before-schema'
# When method POST
# Then status 400
# * match response contains "Schema not found"

# ------------------------------------------------------------------------
Scenario: Store minimal schema
# ------------------------------------------------------------------------
Given request read('/schema/minimal.json')
And path 'content/sites/example.com/schema/test/minimal'
When method POST
Then status 200

# ------------------------------------------------------------------------
Scenario: Store content that uses minimal schema
# ------------------------------------------------------------------------
Given request read('/content/minimal-content.json')
And path 'content/sites/example.com/branches/authoring/elements/somepath/minimal'
When method POST
Then status 200

# ------------------------------------------------------------------------
Scenario: Verify content
# ------------------------------------------------------------------------
Given path 'content/sites/example.com/branches/authoring/elements/somepath/minimal'
When method GET
Then status 200
And match response == read('/content/minimal-content.json')

# ------------------------------------------------------------------------
Scenario: Attempt to store content that the schema does not validate
# ------------------------------------------------------------------------
Given request read('/content/invalid-minimal-content.json')
And path 'content/sites/example.com/branches/authoring/elements/somepath/willfail'
When method POST
Then status 400
* match response contains "$.extra: is not defined in the schema"