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
const fetch = require("node-fetch");
const fs = require("fs");
const mod_path = require("path");
const FormData = require("form-data");
const glob = require("glob");
const winston = require("winston");

/**
 * A class for interacting with the Sling Post Servlet.
 */
class SlingPost {
  /**
   * Construct a new SlingPost instance
   *
   * @param {*} config
   * @param {string} [config.url] the url of the Apache Sling instance to connect to
   * @param {string} [config.username] the username to authenticate with Apache Sling
   * @param {string} [config.password] the password to authenticate with Apache Sling
   * @param {string} [config.base] the base directory to use when creating file paths
   * @param {string} [config.level] the logging level for configuring the logger
   */
  constructor(config) {
    this.config = {
      ...{
        url: "http://localhost:8080",
        username: "admin",
        password: "admin",
        base: "./dist",
        level: "info",
      },
      ...config,
    };

    this.log = winston.createLogger({
      level: this.config.level,
      format: winston.format.simple(),
      transports: [
        new winston.transports.Console({
          format: winston.format.cli(),
        }),
      ],
    });
  }

  /**
   * Add the automatic properties into the params
   *
   * @see https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#automatic-property-values-last-modified-and-created-by
   *
   * @param {Object} params
   */
  static addAutomaticProperties(params) {
    return {
      ...{
        "jcr:created": "",
        "jcr:createdBy": "",
        "jcr:lastModified": "",
        "jcr:lastModifiedBy": "",
      },
      ...params,
    };
  }

  /**
   * Calculates the Sling repository path for a file based on the configured base path
   *
   * @param {string} file the file for which to calculate the repository path
   */
  repositoryPath(file) {
    this.log.silly(`repositoryPath(${file})`);
    const filePath = mod_path.resolve(file);
    const basePath = mod_path.resolve(this.config.base);
    const resolvedPath = filePath.replace(basePath, "");
    this.log.silly(`Resolved path: ${resolvedPath}`);
    return resolvedPath;
  }

  /**
   * Copies the item addressed by the fromPath parameter to a new location indicated by the toPath parameter
   *
   * @see https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#copying-content-1
   *
   * @param {string} fromPath the absolute path to the item to move
   * @param {string} toPath the absolute or relative path to which the resource is copied. If the path is relative it is assumed to be below the same parent as the request resource. If it is terminated with a / character the request resource is copied to an item of the same name under the destination path.
   *
   * @returns {Promise<Object>} the response from Sling with the status, statusText and body
   */
  async copy(fromPath, toPath) {
    this.log.info(`Copying: ${fromPath} to ${toPath}`);
    return this.post(fromPath, {
      ":operation": "copy",
      ":dest": toPath,
    });
  }

  /**
   * Remove existing content
   *
   * @see https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#content-removal-1
   *
   * @param {string} path The absolute path of the item to delete
   * @returns {Promise<Object>} the response from Sling with the status, statusText and body
   */
  async delete(path) {
    this.log.info(`Deleting: ${path}`);
    return this.post(path, {
      ":operation": "delete",
    });
  }

  /**
   * Import content into the Sling repository from a string.
   *
   * @see https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#importing-content-structures-1
   *
   * @param {string} content Specifies content string to import. The format of the import content is the same as is used by the jcr.contentloader bundle.
   * @param {string} path The absolute path of the parent item under which to import the content
   * @param {string} name The name of the content item to create
   * @param {string} [contentType] Specifies the type of content being imported. Possible values are: xml, jcr.xml, json, jar, zip
   * @param {boolean} [replace] Specifies whether the import should replace any existing nodes at the same path. Note: When true, the existing nodes will be deleted and a new node is created in the same place.
   * @param {boolean} [replaceProperties] Specifies whether the import should replace properties if they already exist.
   * @returns {Promise<Object>} the response from Sling with the status, statusText and body
   */
  async importContent(
    content,
    path,
    name,
    contentType = "json",
    replace = true,
    replaceProperties = true
  ) {
    this.log.info(`Importing content to: ${path}`);
    return this.post(path, {
      ":operation": "import",
      ":content": content,
      ":contentType": contentType,
      ":name": name,
      ...(replace && { ":replace": "true" }),
      ...(replaceProperties && { ":replaceProperties": "true" }),
    });
  }

  /**
   * Import content into the Sling repository from a file or multiple files (based on glob)
   *
   * @see https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#importing-content-structures-1
   *
   * @param {string} file Specifies a file uploaded for import. The format of the import content is the same as is used by the jcr.contentloader bundle.
   * @param {string} [path] The absolute path of the parent item under which to import the content. If not specified, the base path will be used to calculate the repository path.
   * @param {string} [name] The absolute path of the parent item under which to import the content. If not specified, filename without extension will be used as a default.
   * @param {string} contentType Specifies the type of content being imported. Possible values are: xml, jcr.xml, json, jar, zip
   * @param {boolean} replace Specifies whether the import should replace any existing nodes at the same path. Note: When true, the existing nodes will be deleted and a new node is created in the same place.
   * @param {boolean} replaceProperties Specifies whether the import should replace properties if they already exist.
   */
  async importFile(
    file,
    path,
    name,
    contentType = "json",
    replace = true,
    replaceProperties = true
  ) {
    this.log.info(`Importing files: ${file}`);
    const files = glob.sync(file);
    for (let idx in files) {
      const f = files[idx];
      const p =
        path || this.repositoryPath(decodeURI(mod_path.dirname(f)) + "/");
      const n = name || decodeURI(mod_path.parse(f).name);
      this.log.debug(`Importing file: ${f} to ${p}`);
      await this.post(p, {
        ":operation": "import",
        ":contentFile": fs.createReadStream(f),
        ":contentType": contentType,
        ":name": n,
        ...(replace && { ":replace": "true" }),
        ...(replaceProperties && { ":replaceProperties": "true" }),
      });
    }
    this.log.info(`Successfully imported: ${files.length} files`);
  }

  /**
   * Moves the item addressed by the fromPath to a new location indicated by the toPath parameter.
   *
   * @see https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#moving-content
   *
   * @param {string} fromPath the absolute path to the item to move
   * @param {string} toPath the absolute or relative path to which the resource is moved. If the path is relative it is assumed to be below the same parent as the request resource. If it is terminated with a / character the request resource is moved to an item of the same name under the destination path.
   *
   * @returns {Promise<Object>} the response from Sling with the status, statusText and body
   */
  async move(fromPath, toPath) {
    this.log.info(`Moving: ${fromPath} to ${toPath}`);
    return this.post(fromPath, {
      ":operation": "move",
      ":dest": toPath,
    });
  }

  /**
   * Sends a POST request to the Apache Sling Post Servlet
   *
   * @param {string} path the path to execute the command
   * @param {Object} params the parameters to send to the Apache Sling Post API
   *
   * @returns {Promise<Object>} the response from Sling with the status, statusText and body
   */
  async post(path, params) {
    this.log.debug(`Posting to: ${path}`);

    this.log.silly(`Sending parameters: ${JSON.stringify(params)}`);
    const formData = new FormData();
    Object.keys(params).forEach((key) => {
      if (Array.isArray(params[key])) {
        for (const val of params[key]) {
          formData.append(key, val);
        }
      } else {
        formData.append(key, params[key]);
      }
    });
    const response = await fetch(`${this.config.url}${path}`, {
      method: "POST",
      headers: {
        Authorization: `Basic ${Buffer.from(
          this.config.username + ":" + this.config.password
        ).toString("base64")}`,
        Accept: "application/json",
      },
      body: formData,
    });
    if (!response.ok) {
      const body = await response.text();
      this.log.silly(`Retrieved error response: ${body}`);
      throw new Error(
        `Failed with invalid status: ${response.status} - ${response.statusText}`
      );
    } else {
      this.log.debug(
        `Post successful: ${response.status} - ${response.statusText}`
      );
      const body = await response.text();
      this.log.silly(`Retrieved response: ${body}`);
      return {
        ok: response.ok,
        status: response.status,
        statusText: response.statusText,
        body,
      };
    }
  }

  /**
   * Upload a file into the Apache Sling repository
   *
   * @see https://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html#file-uploads
   *
   * @param {string} file The file or glob of files to upload into the Apache Sling repository
   * @param {string} [path] The path under which to upload the file. If not specified, the base path will be used to calculate the repository path.
   * @param {Object} [params] Additional parameters to send to the Apache Sling Post API
   */
  async uploadFile(file, path, params, fileName) {
    this.log.info(`Uploading files: ${file}`);
    const files = glob.sync(file);
    for (let idx in files) {
      const f = files[idx];
      const p = path || this.repositoryPath(mod_path.dirname(f));
      const fn = fileName || mod_path.basename(f);
      this.log.debug(`Uploading file: ${f} to ${p}`);
      const formData = new FormData();
      if (params) {
        this.log.silly(`Sending parameters: ${JSON.stringify(params)}`);
        Object.keys(params).forEach((key) => {
          formData.append(key, params[key]);
        });
      }
      formData.append("*", fs.createReadStream(f), { filename: fn });
      const response = await fetch(`${this.config.url}${p}`, {
        method: "POST",
        headers: {
          Authorization: `Basic ${Buffer.from(
            this.config.username + ":" + this.config.password
          ).toString("base64")}`,
          Accept: "application/json",
        },
        body: formData,
      });
      if (!response.ok) {
        throw new Error(
          `Failed with invalid status: ${response.status} - ${response.statusText}`
        );
      } else {
        this.log.debug(
          `Post successful: ${response.status} - ${response.statusText}`
        );
        const body = await response.text();
        this.log.silly(`Retrieved response: ${body}`);
      }
    }
    this.log.info(`Successfully uploaded: ${files.length} files`);
  }
}

module.exports = SlingPost;
