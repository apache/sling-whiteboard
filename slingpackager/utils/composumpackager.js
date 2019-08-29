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

const list = (url, username, password) => {
    logger.log('Listing packages on', url);
    listPackages(url, username, password, '');
}

const uploadPackage = (url, username, password, packagePath, install) => {
    logger.log('Uploading package', packagePath, 'on', url);

    let serviceURL = url + uploadEndpoint;
    logger.debug('Service call: ', serviceURL);
    let post = request.post({ url: serviceURL }, (error, response, body) => {
        if (error) {
            logger.error(error);
        }

        if (response && response.statusCode === 200) {
            var json = JSON.parse(body);
            logger.log(json.status)
            logger.debug(JSON.stringify(json));

            if(install) {
                logger.debug('installing', json.path);
                installPackage(url, username, password, json.path);
            }

        } else {
            logger.error('Unable to upload package. statusCode:', response && response.statusCode);
            logger.debug(body);
        }
    }).auth(username, password);

    post.form().append('file', fs.createReadStream(packagePath));

    logger.debug(JSON.stringify(post.toJSON()));
}

const deletePackage = (url, username, password, package) => {
    logger.log('Deleting package', package, 'on', url);

    let serviceURL = url + deleteEndpoint + package;
    logger.debug('Service call: ', serviceURL);
    let req = request({ url: serviceURL, method: 'DELETE' }, (error, response, body) => {
        if (error) {
            logger.error(error);
        }

        if (response && response.statusCode === 200) {
            if (body) {
                var json = JSON.parse(body);
                logger.log(json.status)
            } else {
                logger.error('Unable to delete package. Check package name (try by path).');
            }
        } else {
            logger.error('Unable to delete package. statusCode:', response && response.statusCode);
            logger.debug(body);
        }
    }).auth(username, password);

    logger.debug(JSON.stringify(req.toJSON()));
}

const installPackage = (url, username, password, package) => {
    logger.log('Installing package', package, 'on', url);

    let serviceURL = url + installEndpoint + package;
    logger.debug('Service call: ', serviceURL);
    let post = request.post({ url: serviceURL }, (error, response, body) => {
        if (error) {
            logger.error(error);
        }

        if (response && response.statusCode === 200) {
            if (body) {
                var json = JSON.parse(body);
                logger.log(json.status)
            }
        } else {
            logger.error('Unable to install package. statusCode:', response && response.statusCode);
            logger.debug(body);
        }
    }).auth(username, password);

    logger.debug(JSON.stringify(post.toJSON()));
}

const uninstallPackage = (url, username, password, package) => {
    logger.log('Uninstalling package', package, 'on', url);

    let serviceURL = url + '/bin/cpm/core/jobcontrol.job.json';
    logger.debug('Service call: ', serviceURL);
    // Commented out code bellow does not work wirh Composum 1.7/Sling9
    // var post = request.post({ url: url + uninstallEndpoint + package }, (error, response, body) => {
    var post = request.post({ url: serviceURL }, (error, response, body) => {
        if (error) {
            logger.error(error);
        }

        if (response && response.statusCode === 200) {
            if (body) {
                var json = JSON.parse(body);
                logger.log('done');
                // Commented out for Composum 1.7/Sling9
                // logger.log(json.status)
            }
        } else {
            logger.error('Unable to uninstall package. statusCode:', response && response.statusCode);
            logger.debug(body);
        }
    }).auth(username, password);

    // These parameters are not needed when using uninstallEndpoint with Sling11.
    // This is a workaround for Composum 1.7 and Sling9 which does not have this endpoint.
    var form = post.form();
    form.append('event.job.topic', 'com/composum/sling/core/pckgmgr/PackageJobExecutor');
    form.append('reference', package);
    form.append('_charset_', 'UTF-8');
    form.append('operation', 'uninstall');

    logger.debug(JSON.stringify(post.toJSON()));
}

const getName = () => {
    return 'Composum Package Manager';
}

function listPackages(url, username, password, path) {
    var serviceURL = url + listEndpoint + path;
    logger.debug('Service call: ', serviceURL);

    let req = request.get({ url: serviceURL}, (error, response, body) => {
        if (error) {
            logger.error(error);
        }

        if (response && response.statusCode === 200) {
            var json = JSON.parse(body);
            
            // Check for structure diff between Composume in Sling9 and Sling11
            var packages = json.children ? json.children : json;
            displayPackages(url, username, password, packages);
        } else {
            logger.error('Unable to connect to server. statusCode:', response && response.statusCode);
            logger.debug(body);
        }

    }).auth(username, password);

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

module.exports = {
    checkService,
    list,
    uploadPackage,
    deletePackage,
    installPackage,
    uninstallPackage,
    getName
}

