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

'use strict';

 const { openWhiskRenderer } = require('./openwhisk-renderer');

 /* eslint-disable no-console */

const defaultTextRenderer = {
  contentType: 'text/plain',
  getRendererInfo : (resourceType, extension) => {
    return extension == 'txt';
  },
  render : (resource) => {
    return { output: `${resource.title}\n${resource.body}\n` };
  },
}

const defaultJsonRenderer = {
  contentType: 'application/json',
  getRendererInfo : (resourceType, extension) => {
    return extension == 'json';
  },
  render : (resource) => {
    return { output: JSON.stringify(resource, 2, null) };
  },
}

const defaultHtmlRenderer = {
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
    </body>
    </html>
  `};
  },
}

const renderers = [
  openWhiskRenderer,
  defaultTextRenderer,
  defaultHtmlRenderer,
  defaultJsonRenderer
];

async function selectRendererInfo(resourceType, extension) {
  return new Promise(async resolve => {
    let i;
    let resolved;
    for(i in renderers) {
      const rendererInfo = await renderers[i].getRendererInfo(resourceType, extension);
      if(rendererInfo) {
        resolve({
          'renderer': renderers[i],
          'rendererInfo': rendererInfo,
        });
        resolved = true;
        break;
      }
    }
    if(!resolved) {
      resolve();
    }
  })
}

async function render(context) {
  const resource = context.content.resource.content;
  const { resourceType } = resource;
  const { extension } = context.request;
  if(context.debug) {
    console.log(`rendering for resourceType ${resourceType} extension ${extension}`);
  }
  const rendererInfo = await selectRendererInfo(resourceType, extension);
  if(context.debug) {
    console.log(rendererInfo);
  }
  if(!rendererInfo) {
    throw Error(`Renderer not found for ${resourceType} extension ${extension}`);
  }
  const rendered = await rendererInfo.renderer.render(resource, rendererInfo.rendererInfo);
  if(!rendered.output) {
    throw Error('Renderer generated no output');
  }
  context.response.body = rendered.output;
  context.response.headers = {
    'Content-Type': rendererInfo.renderer.contentType
  };

  return context;
}

module.exports.render = render;