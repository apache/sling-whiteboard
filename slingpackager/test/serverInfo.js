/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**  
 * test/serverInfo.js
 * 
 * This script can be used to discover some of server information dynamically and/or start test 
 * server instance when running test.
 * 
 * TODO: for now all values are hardcoded.
 */

// default Sling port
const server = 'http://localhost:8080';

// default AEM author port
// const server = 'http://localhost:4502';

// Composum package name
const packServerName = '/slingpackager/testContent-1.0-SNAPSHOT.zip';

// AEM package name
// const packServerName = 'testContent';

const packServerPath = '/etc/packages/slingpackager/testContent-1%2E0-SNAPSHOT.zip';
const testInstallPath = '/content/slingpackager/test.json';
const username = 'admin';
const password = 'admin';

module.exports = {
    server,
    packServerPath,
    packServerName,
    testInstallPath,
    username,
    password
}