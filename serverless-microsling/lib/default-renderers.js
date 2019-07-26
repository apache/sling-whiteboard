/* 
 * Copyright 2019 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* eslint-disable no-console */
'use strict';

module.exports.renderers = {
   text: {
    contentType: 'text/plain',
    getRendererInfo : (resourceType, extension) => {
      return extension == 'txt';
    },
    render : (resource) => {
      return { output: `${resource.title}\n${resource.body}\n` };
    },
  },
  json: {
    contentType: 'application/json',
    getRendererInfo : (resourceType, extension) => {
      return extension == 'json';
    },
    render : (resource) => {
      return { output: JSON.stringify(resource, 2, null) };
    },
  },
  html: {
    contentType: 'text/html',
    getRendererInfo : (resourceType, extension) => {
      return extension == 'html';
    },
    render : (resource) => {
      return { output: `
      <html>
      <head>
      <title>${resource.title}</title>
      </head>
      <body>
      <h1>
          ${resource.title}
      </h1>
      <div>${resource.body}</div>
      <div style="color:blue">This is the default HTML rendering</div>
      </body>
      </html>
    `};
    },
  },
};