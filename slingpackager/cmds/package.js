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
const archiver = require('archiver')
const fs = require('fs')
const path = require('path')
const xmlbuilder = require('xmlbuilder');
const logger = require('../utils/consoleLogger')

const configFile = 'slingpackager.config.js'

exports.command = 'package <folder>'
exports.desc = 'create a package'
exports.builder = {
  server: {
    hidden: true
  },
  user: {
    hidden: true
  },
  retry: {
    hidden: true
  },
  destination: {
    alias: 'd',
    describe: 'Package destination directory. Defaults to current directory.'
  },
  config: {
    alias: 'c',
    describe: 'Package configuration/properties. Package properties.xml and package name are generated from this. ' +
      'If this option is missing slingpackage will search for slingpackager.config.js in the parent directories.'
  }
}
exports.handler = (argv) => {
  logger.init(argv);
  
  if(isValid(argv.folder)) {
    archive(argv);
  }
}

function archive(argv) {
  var config = loadConfig(argv);
  if(config === undefined) {
    logger.error(configFile,'not found in the project.');
    throw "Unable to find configuration " + configFile;
  }

  var destDir = process.cwd();
  if(argv.destination && 
    fs.existsSync(argv.destination) && 
    fs.statSync(argv.destination).isDirectory()) {
      destDir = argv.destination
  }

  var packagePath = path.join(destDir, packageName(config));

  logger.log('package folder', argv.folder, 'as', packagePath);
  var output = fs.createWriteStream(packagePath);
  var archive = archiver('zip');

  archive.on('error', (err) => { throw err });

  archive.pipe(output);

  var jcrRoot = path.join(argv.folder, 'jcr_root');
  archive.directory(jcrRoot, 'jcr_root', { name: 'jcr_root' });

  var metainf = path.join(argv.folder, 'META-INF');
  archive.directory(metainf, 'META-INF', { name: 'META-INF' });

  addConfigDefaults(config);
  var xml = propertiesXMLFromJson(config);
  logger.debug('Writing generated META-INF/vault/properties.xml');
  logger.debug(xml);
  archive.append(xml, {name: 'META-INF/vault/properties.xml'});

  archive.finalize();
}

function isValid(dir) {
  if(!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) {
    console.log(dir,"does not exist or is not a folder.");
    return false;
  }

  var jcrRoot = path.join(dir, 'jcr_root');
  if(!fs.existsSync(jcrRoot) || !fs.statSync(jcrRoot).isDirectory()) {
    console.log(jcrRoot,"does not exist or is not a folder.");
    return false;
  }

  var metainf = path.join(dir, 'META-INF');
  if(!fs.existsSync(metainf) || !fs.statSync(metainf).isDirectory()) {
    console.log(metainf,"does not exist or is not a folder.");
    return false;
  }

  return true;
}

function propertiesXMLFromJson(json) {
  var properties = json['vault-properties'];
  var entries = properties['entry'];
  var xml = xmlbuilder.create('properties'); 

  for(var elem in properties) {
    if(elem != 'entry') {
      xml.ele(elem, properties[elem]);
    }
  }

  for(var entry in entries) {
    xml.ele('entry', {'key': entry}, entries[entry]);
  }

  xml.end({ pretty: true});
  
  var xmlProlog = '<?xml version="1.0" encoding="UTF-8" standalone="no"?>\n' 
    + '<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">\n';

  return xmlProlog + xml.toString({ pretty: true});
}

function addConfigDefaults(config) {
  var properties = config['vault-properties'];
  var entries = properties['entry'];

  if(!entries['createdBy']) {
    entries['createdBy'] = 'slingpackager';
  }

  if(!entries['acHandling']) {
    entries['acHandling'] = 'IGNORE';
  }

  if(!entries['allowIndexDefinitions']) {
    entries['allowIndexDefinitions'] = false;
  }

  if(!entries['requiresRoot']) {
    entries['requiresRoot'] = false;
  }

  if(!entries['path']) {
    entries['path'] = '/etc/packages/' 
       + entries['group'] 
       + '/' + entries['name'] 
       + '-' + entries['version'] 
       + '.zip';
  }
}

function packageName(config) {
  var properties = config['vault-properties'];
  var entries = properties['entry'];
  var name = entries['name'];
  var group = entries['group'];
  var version = entries['version'];

  if(!name || !group || !version) {
    logger.error(configFile,
      "is missing one or more of the required entries for 'name', 'group' or 'version' to generate a package.");
    throw "Config is missing one or more of the required entries for 'name', 'group' or 'version' to generate a package."
  }

  return name+"-"+version+".zip";
}

function loadConfig(argv) {
  var configFile 
  if(argv.config) {
    if(fs.existsSync(argv.config) && fs.statSync(argv.config).isFile) {
      configFile = argv.config;
    } else {
      logger.warn('Unable to find configuration file',argv.config);
    }
  } 
  
  if(configFile === undefined) {
    configFile = findConfigFile(argv.folder);
  }

  if(configFile != undefined) {
    var config = requireConfig(configFile);
    if(config === undefined) {
      config = JSON.parse(fs.readFileSync(configFile, 'utf8'));
    }

    logger.debug(JSON.stringify(config));
    return config;
  } else {
    logger.error('Unable to find configuration file either in parent folders or provided via --config option.');
  }

  return undefined;
}

function requireConfig(configFile) {
  try {
    return require(configFile);
  } catch(err) {}

  return undefined;
}

function findConfigFile(dirPath) {
  logger.debug('Looking for', configFile, 'in', dirPath);
  var filePath = path.join(dirPath, configFile);
  if(fs.existsSync(filePath)) {
    logger.debug('Found', filePath);
    return filePath;
  } else {
    var parentPath = path.resolve(dirPath, '..');
    if(parentPath && parentPath != dirPath) {
      return findConfigFile(parentPath);
    }
  }

  logger.warn('Unable to find',configFile);
  return undefined;
}