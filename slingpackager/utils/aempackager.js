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
const xml2js = require('xml2js')
const util = require('util')
const logger = require('./consoleLogger')

const endpoint = "/crx/packmgr/service.jsp";

const checkService = (url, username, password, callback) => {
    let serviceURL = url + endpoint;
    logger.debug('Checking AEM Package Manager.');
    logger.debug('Service call: ', serviceURL);

    let req = request.get({url: serviceURL}, (error, response, body) => {
        if(response && response.statusCode===200) {
            callback(true);
        } else {
            callback(false)
        }
    }).auth(username, password);

    logger.debug('request =', JSON.stringify(req.toJSON()));
}

const list = (url, username, password) => {
  logger.log('Listing packages on', url)

  let serviceURL = url + endpoint + '?cmd=ls';
  logger.debug('Service call: ', serviceURL);
  let post = request.post({url: serviceURL}, (error, response, body) => {
    if(error) {
      logger.error(error);
    }

    if(response && response.statusCode===200) {
      // logger.log(body);
      xml2js.parseString(body, (error, result) => {
        if(result) {
          var data = getData(result);
          displayPackages(data[0].packages[0].package);
        } else if(error) {
          logger.error(error);
        }
      });
    } else {
      logger.error('Unable to connect to server. statusCode:', response && response.statusCode);
    }

  }).auth(username, password);

  logger.debug(JSON.stringify(post.toJSON()));
}

const uploadPackage = (url, username, password, packagePath, install) => {
  logger.log('Uploading AEM package',packagePath,'on', url);

  let serviceURL = url + endpoint;
  if(install) {
    serviceURL = serviceURL + '?install=true'
  }

  logger.debug('Service call: ', serviceURL);
  var post = request.post({url: serviceURL}, (error, response, body) => {
    if(error) {
      logger.error(error);
    }

    if(response && response.statusCode===200) {
      xml2js.parseString(body, (error, result) => {
        if(result) {
          if(getStatusCode(result) === '200') {
            logger.log("Done!");
          } else {
            logger.error("Something went wrong! Check server logs.");
          }
        } else if(error) {
          logger.error(error);
        }
        logger.debug(body);
      });
    } else {
      logger.error('Unable to connect to server. statusCode:', response && response.statusCode);
    }

  }).auth(username, password);
  post.form().append('file', fs.createReadStream(packagePath));
  logger.debug(JSON.stringify(post.toJSON()));
}

const deletePackage = (url, username, password, package) => {
  logger.log('Deleting AEM package',package,'on', url);
  let post = executePackageCommand(url, username, password, package, 'rm');
  logger.debug(JSON.stringify(post.toJSON()));
}

const installPackage = (url, username, password, package) => {
  logger.log('Installing AEM package',package,'on', url);
  let post = executePackageCommand(url, username, password, package, 'inst');
  logger.debug(JSON.stringify(post.toJSON()));
}

const uninstallPackage = (url, username, password, package) => {
  logger.log('Uninstalling AEM package',package,'on', url);
  let post = executePackageCommand(url, username, password, package, 'uninst');
  logger.debug(JSON.stringify(post.toJSON()));
}

const getName = () => {
  return 'AEM Package Manager';
}

function executePackageCommand(url, username, password, packageName, cmd) {
  let serviceURL = url + endpoint + '?cmd=' + cmd;
  logger.debug('Service call: ', serviceURL);
  var post = request.post({url: serviceURL}, (error, response, body) => {
    if(error) {
      logger.error(error);
    }

    if(response && response.statusCode===200) {
      xml2js.parseString(body, (error, result) => {
        if(result) {
          if(getStatusCode(result) === '200') {
            logger.log("Done!");
          } else {
            logger.log(getStatusText(result));
          }
        } else if(error) {
          logger.error(error);
        }
      });
    } else {
      logger.error('Unable to connect to server. statusCode:', response && response.statusCode);
    }

  }).auth(username, password);
  post.form().append('name', packageName);

  return post;
}

function displayPackages(packages) {
  for(var i=0; i<packages.length; i++) {
    logger.log('name='+packages[i].name[0]+
       ' group='+packages[i].group[0]+
       ' version='+packages[i].version[0]+
       ' path='+packages[i].downloadName[0]);
  }
}

function getStatusCode(result) {
  return result.crx.response[0].status[0].$.code;
}

function getStatusText(result) {
  return result.crx.response[0].status[0]._;
}

function getData(result) {
  return result.crx.response[0].data;
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

