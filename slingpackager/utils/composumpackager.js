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
const request = require('request')
const fs = require('fs')
const path = require('path')
const logger = require('./consoleLogger');

const listEndpoint = "/bin/cpm/package.list.json";
const uploadEndpoint = "/bin/cpm/package.upload.json";
const deleteEndpoint = "/bin/cpm/package.delete.json";
const installEndpoint = "/bin/cpm/package.install.json";
const uninstallEndpoint = "/bin/cpm/package.uninstall.json";

const checkService = (url, username, password, callback) => {
    let serviceURL = url + '/bin/cpm/package.json';
    logger.debug('Checking Composum Package Manager.');
    logger.debug('Service call: ', serviceURL);
    request.get({ url: serviceURL }, (error, response, body) => {
        if (response && response.statusCode === 200) {
            callback(true);
        } else {
            callback(false)
        }

    }).auth(username, password);
}

const list = (url, username, password, maxRetry) => {
    logger.log('Listing packages on', url);
    listPackages(url, username, password, '', maxRetry);
}

const uploadPackage = (url, username, password, packagePath, install, maxRetry) => {
    logger.log('Uploading package', packagePath, 'on', url);

    let serviceURL = url + uploadEndpoint;
    let post = callPostService({serviceURL, username, password, maxRetry}, (error, json) => {
        if(error) {
            logger.error('Unable to upload package', packagePath);
            logger.error(error);
            process.exit(1);
        } else {
            logger.log(json.status)
            logger.debug(JSON.stringify(json));

            if(install) {
                logger.info('installing', json.path);
                installPackage(url, username, password, json.path, maxRetry);
            }
        }
    });

    post.form().append('file', fs.createReadStream(packagePath));

    logger.debug(JSON.stringify(post.toJSON()));
}

const deletePackage = (url, username, password, package, maxRetry) => {
    logger.log('Deleting package', package, 'on', url);

    let serviceURL = url + deleteEndpoint + package;
    let req = callService({serviceURL, method: 'DELETE', username, password, maxRetry}, (error, json) => {
        if(error) {
            logger.error('Unable to delete package', package);
            logger.error(error);
            process.exit(1);
        } else {
            logger.log(json.status);
            logger.debug(JSON.stringify(json));
        }
    });

    logger.debug(JSON.stringify(req.toJSON()));
}

const installPackage = (url, username, password, package, maxRetry) => {
    logger.log('Installing package', package, 'on', url);

    let post = postJob({url, username, password, package, maxRetry}, 'install', (error, result) => {
        if(error) {
            logger.error('Unable to uninstall package', package);
            logger.error(error);
            process.exit(1);
        } 
    });
}

const uninstallPackage = (url, username, password, package, maxRetry) => {
    logger.log('Uninstalling package', package, 'on', url);

    let post = postJob({url, username, password, package, maxRetry}, 'uninstall', (error, result) => {
        if(error) {
            logger.error('Unable to uninstall package', package);
            logger.error(error);
            process.exit(1);
        } else {
            if(result && (typeof(result) === 'string') && result.startsWith('Unable')) {
                logger.error(result);
                process.exit(1);
            }
        }
    });
}

const buildPackage = (url, username, password, package, maxRetry) => {
    logger.log('Building package', package, 'on', url);

    let post = postJob({url, username, password, package, maxRetry}, 'assemble', (error, result) => {
        if(error) {
            logger.error('Unable to build package', package);
            logger.error(error);
            process.exit(1);
        } else {
            if(result && (typeof(result) === 'string') && result.startsWith('Unable')) {
                logger.error(result);
                process.exit(1);
            }
        }
    });
}

const getName = () => {
    return 'Composum Package Manager';
}

function listPackages(url, username, password, path, maxRetry) {
    var serviceURL = url + listEndpoint + path;
    
    let req = callGetService({serviceURL, username, password, maxRetry}, (error, json) => {
        if(error) {
            logger.error("Unable to list packages.");
            logger.error(error);
            process.exit(1);
        } else {
            var packages = json.children ? json.children : json;
            displayPackages(url, username, password, packages);
        }
    }); 

    logger.debug(JSON.stringify(req.toJSON()));
}

function displayPackages(url, username, password, packages) {
    for (var i = 0; i < packages.length; i++) {
        if(packages[i].type === 'package') {
            logger.log('name=' + packages[i].definition.name +
                ' group=' + packages[i].definition.group +
                ' version=' + packages[i].definition.version +
                ' path=' + packages[i].id);
        } else if(packages[i].type === 'folder') {
            // This is not needed on Sling11 as list service returns complete list of packages.
            listPackages(url, username, password, packages[i].path);
        }
    }
}

function postJob(data, operation, callback) {
    data.serviceURL = data.url + '/bin/cpm/core/jobcontrol.job.json';
    let post = callPostService(data, (error, json) => {
        if(json && json['slingevent:eventId']) {
            setTimeout(() => {
                getJobOutput(data.url, data.username, data.password, json['slingevent:eventId'], callback)
            },100);
        } else {
            callback(error, json); 
        }
    });

    var form = post.form();
    form.append('event.job.topic', 'com/composum/sling/core/pckgmgr/PackageJobExecutor');
    form.append('_charset_', 'UTF-8');
    form.append('operation', operation);
    form.append('reference', data.package);

    return post;
}

function callGetService(data, callback) {
    data.method = "GET";
    return callService(data, callback);
}

function callPostService(data, callback) {
    data.method = "POST";
    var post = callService(data, callback);
    logger.debug('POST:', JSON.stringify(post.toJSON(), undefined, '   '));
    return post;
}

function callService(data, callback) {
    if(data.retryCount === undefined) {
        data.retryCount = 0;
        if(data.maxRetry === undefined) {
            data.maxRetry = 10;
        }
    }

    logger.debug(data.retryCount + '. Service call: ', data.serviceURL);

    let req = request({ url: data.serviceURL, method: data.method }, (error, response, body) => {
        var statusCodeLine = (response === undefined) ? "" : "Response: " + response.statusCode + " : " + response.statusMessage;
        logger.debug(statusCodeLine);

        if (error) {
            if(data.retryCount < data.maxRetry) {
                data.retryCount++;
                callService(data, callback);
            } else  { 
                logger.error(error);
                callback(error + " " + statusCodeLine, undefined); 
            }

            return;
        }

        if (response && response.statusCode === 200) {
            if (body) {
                var json = JSON.parse(body);
                logger.debug('Response:', JSON.stringify(json, undefined, '   '));
                callback(undefined, json);
                return;
            } else {
                logger.debug("Respons has no body.");
            }
        }

        if(data.retryCount < data.maxRetry) {
            data.retryCount++;
            callService(data, callback);
        } else  { 
            callback("Error calling service " + data.serviceURL, undefined);
        }

        return;
    }).auth(data.username, data.password);
    return req;
}

function getJobOutput(url, username, password, eventId, callback, jobState) {
    var requestData = {url: url + "/bin/cpm/core/jobcontrol.outfile.txt/" + eventId};
    if(jobState === undefined || jobState === "ACTIVE" || jobState === "QUEUED") {
        requestData.url = url + "/bin/cpm/core/jobcontrol.job.json/" + eventId;
    } 

    request.get(requestData, (error, response, body) => {
        var statusCodeLine = (response === undefined) ? "" : "Response: " + response.statusCode + " : " + response.statusMessage;
        logger.debug(statusCodeLine);

        if(error) {
            logger.error(error);
        } else if(body) {
            if(body.trim().startsWith("{")) {
                var json = JSON.parse(body);
                logger.debug('Response:', JSON.stringify(json, undefined, '   '));
                if(json["jobState"]) {
                    setTimeout(()=>{
                        getJobOutput(url, username, password, eventId, callback, json["jobState"]);
                    }, 100);
                    return;
                }
            } 
            
            logger.log(body.trim());
        } else if (response && response.statusCode != 200) {
            if(callback) {
                callback('Package manager job service failed. '+statusCodeLine, undefined);
            } else {
                logger.error('Package manager job service failed.', statusCodeLine);
                process.exit(1);
            }
        }

        if(callback) {
            callback(error, body); 
        } 

    }).auth(username, password);
}

module.exports = {
    checkService,
    list,
    uploadPackage,
    deletePackage,
    installPackage,
    uninstallPackage,
    buildPackage,
    getName
}

