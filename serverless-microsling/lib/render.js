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

 const { openWhiskRenderer } = require('./openwhisk-renderer');

 /* eslint-disable no-console */

const defaultTextRenderer = {
  contentType: 'text/plain',
  appliesTo : (resourceType, extension) => {
    return extension == 'txt';
  },
  render : (resource) => {
    return `${resource.title}\n${resource.body}\n`;
  },
}

const defaultJsonRenderer = {
  contentType: 'application/json',
  appliesTo : (resourceType, extension) => {
    return extension == 'json';
  },
  render : (resource) => {
    return JSON.stringify(resource, 2, null);
  },
}

const defaultHtmlRenderer = {
  contentType: 'text/html',
  appliesTo : (resourceType, extension) => {
    return extension == 'html';
  },
  render : (resource) => {
    return `
    <html>
    <head>
    <title>${resource.title}</title>
    </head>
    <body>
    <h1>
        ${resource.title}
    </h1>
    <div>${resource.body}</div>
    </body>
    </html>
  `;
  },
}

const renderers = [
  openWhiskRenderer,
  defaultTextRenderer,
  defaultHtmlRenderer,
  defaultJsonRenderer
];

async function render(context) {
  const resource = context.content.resource.content;
  if(context.debug) {
    console.log(`rendering for resourceType ${resource.resourceType} extension ${context.request.extension}`);
  }
  const renderer = renderers.find(r => r.appliesTo(resource.resourceType, context.request.extension));
  context.response.body = renderer.render(resource);
  context.response.headers = {
    'Content-Type': renderer.contentType
  };
  return context;
}

module.exports.render = render;