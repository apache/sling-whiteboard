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
const fs = require('fs');
const path = require('path')
const packager = require('../utils/packager')
const logger = require('../utils/consoleLogger')

exports.command = 'upload <package>'
exports.desc = 'upload package to server'
exports.builder = {
  install: {
    alias: 'i',
    describe: 'install the package after it\'s uploaded',
    default: false,
    boolean: true
  }
}
exports.handler = (argv) => {
  logger.init(argv);

  var packagePath = argv.package;
  if(!path.isAbsolute(packagePath)) {
    packagePath = path.join(process.cwd(), packagePath);
  }

  if(!fs.existsSync(packagePath) || !fs.statSync(packagePath).isFile()) {
    logger.error("Valid package path not provided.");
    return;
  }

  let user = argv.user.split(':');
  let userName = user[0];
  let pass = '';
  if(user.length > 1) {
    pass = user[1];
  }

  packager.test(argv, (success, packageManager) => {
    if(success) {
        packageManager.upload(argv.server, userName, pass, packagePath, argv.install, argv.retry);
    }
  });

}