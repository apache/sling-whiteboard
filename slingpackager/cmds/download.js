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
const packager = require('../utils/packager')
const logger = require('../utils/consoleLogger')
const fs = require('fs')
const path = require('path')

exports.command = 'download <package>'
exports.desc = 'download package from server'

exports.builder = {
  destination: {
    alias: 'd',
    describe: 'Package destination directory. Defaults to current directory.'
  }
}

exports.handler = (argv) => {
  let user = argv.user.split(':');
  let userName = user[0];
  let pass = '';
  if(user.length > 1) {
    pass = user[1];
  }

  var destDir = process.cwd();
  if(argv.destination && 
    fs.existsSync(argv.destination) && 
    fs.statSync(argv.destination).isDirectory()) {
      destDir = path.resolve(argv.destination);
  }

  logger.init(argv);
  packager.test(argv, (success, packageManager) => {
    if(success) {
        packageManager.download(argv.server, userName, pass, destDir, argv.package, argv.retry);
    }
  });

}