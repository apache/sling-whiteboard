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
@schema
Feature: Test schema storage
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
Scenario: Attempt to store an invalid schema
# ------------------------------------------------------------------------
Given request read('/schema/invalid.json')
And path 'content/sites/example.com/schema/invalid'
When method POST
Then status 400

# ------------------------------------------------------------------------
Scenario: Store a valid schema
# ------------------------------------------------------------------------
Given request read('/schema/minimal.json')
And path 'content/sites/example.com/schema/minimal'
When method POST
Then status 200

# ------------------------------------------------------------------------
Scenario: Retrieve and check the valid schema
# ------------------------------------------------------------------------
Given path 'content/sites/example.com/schema/minimal'
When method GET
Then status 200
And match response == read('/schema/minimal.json')
