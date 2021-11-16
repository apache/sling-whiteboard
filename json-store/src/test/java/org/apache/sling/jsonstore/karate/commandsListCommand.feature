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
Feature: Test the "commands list" command
# ------------------------------------------------------------------------

# ------------------------------------------------------------------------
Background:
# ------------------------------------------------------------------------

* url baseURL

# Use admin credentials for all requests
* configure headers = call read('classpath:util/basic-auth-header.js')

# ------------------------------------------------------------------------
Scenario: Get list of commands
# ------------------------------------------------------------------------
Given request read('/content/empty.json')
And path 'content/sites/example.com/commands/cmd/list'
When method POST
Then status 200

# TODO better way to match array ignoring the order of elements?
And match response.commands[*] contains read('/commands/list-commands-output.json').commands[0]
And match response.commands[*] contains read('/commands/list-commands-output.json').commands[1]
And match response.commands[*] contains read('/commands/list-commands-output.json').commands[2]