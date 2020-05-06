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
// test/slingpackager.tests.js
const logger = require('../utils/consoleLogger');
const serverInfo = require('./serverInfo');
const fs = require('fs');
const assert = require('assert');
const path = require('path');
const request = require('request');
const exec = require('child_process').execSync;

const packageName = 'testContent-1.0-SNAPSHOT.zip'
const packPath = path.join('test','testContent-1.0-SNAPSHOT.zip');
const packDirPath = path.join('test','resources','test-content');

const server = serverInfo.server;
const packServerPath = serverInfo.packServerPath;
const packServerName = serverInfo.packServerName;
const testInstallPath = serverInfo.testInstallPath;
const username = serverInfo.username;
const password = serverInfo.password;

// uncomment to get more logging
// logger.verbosity(5);

describe('slingpackager', function() {

    this.timeout(30000);
    
    // package test
    describe('package', function() {
        it('should create package', function() {
            testPackage();
        });
    });

    // upload test
    describe('upload', function() {
        it('should upload package', function(done) { 
            testUpload(done); 
        });
    });

    // list test
    describe('list', function() {
        it('should list packages', function() {
            testList();
        });
    });

    // download test
    describe('download', function() {
        it('should download package', function() {
            testDownload();
        });
    });

    // install test
    describe('install', function() {
        it('should install package', function(done) {
            testInstall(done);
        });
    });

    // build test
    describe('build', function() {
        it('should build package', function(done) {
            testBuild(done);
        });
    });

    // uninstall test
    describe('uninstall', function() {
        it('should uninstall package', function(done) {
            testUninstall(done);
        });
    });

    // delete test
    describe('delete', function() {
        it('should delete package', function(done) {
            testDelete(done); 
        });
    });

    after(function() {
        logger.log(exec('rm ' + packPath));
    });
});

// package command test
function testPackage() {
    var cmd = 'node bin/slingpackager package -d test ' + packDirPath;
    var output = exec(cmd);
    assert.equal((fs.existsSync(packPath) && fs.statSync(packPath).size>0), true);
}

// upload command test
function testUpload(done) {
    var cmd = 'node bin/slingpackager upload ' + packPath + ' -s ' + server;
    var output = exec(cmd);
    logger.debug(output);
    assert200(server + packServerPath, done);
}

// list command test
function testList() {
    var cmd = 'node bin/slingpackager list' + ' -s ' + server;
    var output = exec(cmd);
    logger.debug(output);
}

// download command test
function testDownload() {
    var orgSize = fs.statSync(packPath).size;
    cleanup();
    var cmd = 'node bin/slingpackager download ' + packServerName + ' -d test ' + ' -s ' + server;
    var output = exec(cmd);
    logger.debug(output);
    assert.equal((fs.existsSync(packPath) && fs.statSync(packPath).size==orgSize), true);
}

// install command test
function testInstall(done) {
    var cmd = 'node bin/slingpackager install ' + packServerName + ' -s ' + server;
    var output = exec(cmd);
    logger.debug(output);
    assert200(server + testInstallPath, done);
};

// build command test
function testBuild(done) {
    var cmd = 'node bin/slingpackager build ' + packServerName + ' -s ' + server;
    var output = exec(cmd);
    logger.debug(output);
    assert200(server + testInstallPath, done);
};

// uninstall command test
function testUninstall(done) {
    var cmd = 'node bin/slingpackager uninstall ' + packServerName + ' -s ' + server;
    var output = exec(cmd);
    logger.debug(output);
    assert404(server + testInstallPath, done);
};

// delete test
function testDelete(done) {
    var cmd = 'node bin/slingpackager delete ' + packServerName + ' -s ' + server;
    var output = exec(cmd);
    logger.debug(output);
    assert404(server + packServerPath, done);
};

function cleanup() {
    exec('rm ' + packPath);
}

function assert200(testURL, done) {
    setTimeout(() => { assertStatusCode(testURL, 200, done); }, 100);
}

function assert404(testURL, done) {
    setTimeout(() => { assertStatusCode(testURL, 404, done); }, 100);
}

function assertStatusCode(testURL, statusCode, done) {
    logger.debug('assertStatusCode(',testURL,',',statusCode,'assertStatusCode');
    request.get({url: testURL}, function(error, response, body) {
        logResponse(error, response, body);
        
        try {
            assert.equal(response && response.statusCode===statusCode, true);
        } finally {
            if(done) {
                done();
            }
        }
    }).auth(username, password);
}

function logResponse(error, response, body) {
    logger.debug('error ->', error);
    if(response) {
        logger.debug('response ->', response.statusCode);
    }
    // logger.debug('body ->', body);
}