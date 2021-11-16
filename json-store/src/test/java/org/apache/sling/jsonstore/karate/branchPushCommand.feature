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
@commands
Feature: Test the branch push command
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

# ------------------------------------------------------------------------
Scenario: Prepare test content
# ------------------------------------------------------------------------
Given request read('/schema/minimal.json')
And path 'content/sites/example.com/schema/test/minimal'
When method POST
Then status 200

Given request read('/content/minimal-content.json')
And path 'content/sites/example.com/branches/authoring/content/minimal'
When method POST
Then status 200

# ------------------------------------------------------------------------
Scenario: Execute branch push command
# ------------------------------------------------------------------------
Given request read('/commands/branch-push-input.json')
And path 'content/sites/example.com/commands/branch/push'
When method POST
Then status 200
# TODO And match response == read('/commands/ping-output.json')

# ------------------------------------------------------------------------
Scenario: Verify pushed content
# ------------------------------------------------------------------------
Given path '/content/sites/example.com/branches/testing/content/minimal'
When method GET
Then status 200
And match response == read('/content/minimal-content.json')

# ------------------------------------------------------------------------
Scenario: Modify content in authoring branch and verify that testing branch is unchanged
# ------------------------------------------------------------------------
Given request read('/content/minimal-content-2.json')
And path 'content/sites/example.com/branches/authoring/content/minimal'
When method POST
Then status 200

Given path '/content/sites/example.com/branches/authoring/content/minimal'
When method GET
Then status 200
And match response == read('/content/minimal-content-2.json')

Given path '/content/sites/example.com/branches/testing/content/minimal'
When method GET
Then status 200
And match response == read('/content/minimal-content.json')
