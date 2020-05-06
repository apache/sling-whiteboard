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

const check = (url, username, password, callback) => {
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

const list = (url, username, password, maxRetry) => {
  logger.log('Listing packages on', url)

  let serviceURL = url + endpoint + '?cmd=ls';
  var post = callService({serviceURL, username, password, maxRetry}, (error, result) => {
    if(error) {
      logger.error(error);
      process.exit(1);
    }

    if(result) {
      var data = getData(result);
      displayPackages(data[0].packages[0].package);
    }
  });
}

const upload = (url, username, password, packagePath, install, maxRetry) => {
  logger.log('Uploading AEM package',packagePath,'on', url);

  let serviceURL = url + endpoint;
  if(install) {
    serviceURL = serviceURL + '?install=true'
  }

  var post = executeCommand(serviceURL, username, password, maxRetry);
  post.form().append('file', fs.createReadStream(packagePath));
}

const deletePackage = (url, username, password, package, maxRetry) => {
  logger.log('Deleting AEM package',package,'on', url);
  executePackageCommand(url, username, password, package, 'rm', maxRetry);
}

const install = (url, username, password, package, maxRetry) => {
  logger.log('Installing AEM package',package,'on', url);
  executePackageCommand(url, username, password, package, 'inst', maxRetry);
}

const uninstall = (url, username, password, package, maxRetry) => {
  logger.log('Uninstalling AEM package',package,'on', url);
  executePackageCommand(url, username, password, package, 'uninst', maxRetry);
}

const build = (url, username, password, package, maxRetry) => {
  logger.log('Building AEM package', package, 'on', url);
  executePackageCommand(url, username, password, package, 'build', maxRetry);
}

const download = (url, username, password, destination, package, maxRetry) => {
  let serviceURL = url + endpoint + '?cmd=get&name='+package;
  downloadPackage({url, username, password, destination, package, maxRetry});
}

function downloadPackage(data) {
  logger.log('Downloading AEM package', data.package, 'from', data.url, 'to', data.destination);
  if(data.filePath) {
      data.download = true;
      data.serviceURL = data.url + endpoint + '?cmd=get&name='+data.package;
      logger.debug(data.package, "to", data.filePath);
      callService(data, (error, response) => {
          if(error) {
              logger.error("Unable to download package.");
              process.exit(1);
          } else {
              if(fs.existsSync(data.filePath)) {
                  var stats = fs.statSync(data.filePath);
                  logger.log("Package downloaded.");
                  logger.log(stats.size + " " + data.filePath);
              }
          }
      }, true).pipe(fs.createWriteStream(data.filePath));
  } else {
      data.download = false;
      data.serviceURL = data.url + endpoint + '?cmd=ls';
      callService(data, (error, result) => {
          if(error) {
              logger.error("Unable to download package.");
              process.exit(1);
          } else if(result) {
              var packages = getData(result)[0].packages[0].package;
              for (var i = 0; i < packages.length; i++) {
                var pack = packages[i];
                if(pack.name[0] === data.package || pack.downloadName[0] === data.package) {
                  let fileName = packages[i].downloadName[0];
                  let filePath = path.join(data.destination, fileName);
                  data.filePath = filePath;
                  data.package = packages[i].name[0];
                  downloadPackage(data);
                  return;
                }
              }
          }
      }, true);
  }
}

const getName = () => {
  return 'AEM Package Manager';
}

function executePackageCommand(url, username, password, packageName, cmd, maxRetry) {
  let serviceURL = url + endpoint + '?cmd=' + cmd;
  var post = executeCommand(serviceURL, username, password, maxRetry);
  post.form().append('name', packageName);
  return post;
}

function executeCommand(serviceURL, username, password, maxRetry) {
  var post = callService({serviceURL, username, password, maxRetry}, (error, result) => {
    if(error) {
      logger.error(error);
      process.exit(1);
    }

    if(result) {
      var respLog = getResponseLog(result);
      if(respLog) {
        logger.log(respLog);
      } else {
        logger.log(getStatusText(result));
      }
    }
  });

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

  let req = request.post({ url: data.serviceURL }, (error, response, body) => {
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
      } else if(response && response.statusCode===200) {
        if(data.download) {
          callback(undefined, response);
          return;
        }
        xml2js.parseString(body, (error, result) => {
          if(result) {
            logger.debug('Response body: ',body);
            if(getStatusCode(result) === '200') {
              callback(undefined, result);
            } else {
              logger.debug(body);
              logger.warn("Response status:",getStatusCode(result),":",getStatusText(result));
              retryCallService(data, 'Unable to parse service response for', data.serviceURL, callback);
            }
          } else if(error) {
            logger.debug(body);
            logger.warn('Unable to parse service response for', data.serviceURL);
            retryCallService(data, 'Unable to parse service response for', data.serviceURL, callback);
          }
        });

        return;
      } 

      retryCallService(data, "Error calling service " + data.serviceURL, callback)

      return;
  }).auth(data.username, data.password);

  logger.debug(JSON.stringify(req.toJSON()));
  return req;
}

function retryCallService(data, error, callback) {
  if(data.retryCount < data.maxRetry) {
    data.retryCount++;
    callService(data, callback);
   } else  { 
    callback(error, undefined);
   }
}

function displayPackages(packages) {
  for(var i=0; i<packages.length; i++) {
    logger.log('name='+packages[i].name[0]+
       ' group='+packages[i].group[0]+
       ' version='+packages[i].version[0]+
       ' path='+packages[i].downloadName[0]+
       ' size='+packages[i].size[0]);
  }
}

function getStatusCode(result) {
  try {
    return result.crx.response[0].status[0].$.code;
  } catch(e) {
    return undefined;
  }
}

function getStatusText(result) {
  try {
    return result.crx.response[0].status[0]._;
  } catch(e) {
    return undefined;
  }
}

function getResponseLog(result) {
  if(result.crx.response[0].log) {
    return result.crx.response[0].log;
  }
  if(result.crx.response[0].data && result.crx.response[0].data[0].log) {
    return result.crx.response[0].data[0].log;
  } 
  return undefined;
}

function getData(result) {
  return result.crx.response[0].data;
}

module.exports = {
    check,
    list,
    upload,
    deletePackage,
    install,
    uninstall,
    build,
    download,
    getName
}

