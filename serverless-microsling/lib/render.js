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

const { openWhiskRenderer } = require('./openwhisk-renderer');
const defaultRenderers = require('./default-renderers');

// Ordered list of renderers, first ones get preference
const renderers = [
  openWhiskRenderer,
  defaultRenderers.renderers.json,
  defaultRenderers.renderers.html,
  defaultRenderers.renderers.text,
];

async function selectRendererInfo(resourceType, extension) {
  return new Promise(async resolve => {
    let i;
    let result;
    for(i in renderers) {
      const rendererInfo = await renderers[i].getRendererInfo(resourceType, extension);
      if(rendererInfo) {
        result = {
          'renderer': renderers[i],
          'info': rendererInfo,
        };
        break;
      }
    }
    resolve(result);
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
  const rendered = await rendererInfo.renderer.render(resource, rendererInfo.info);
  if(!rendered.output) {
    throw Error('Renderer generated no output');
  }
  context.response.body = rendered.output;
  context.response.headers = {
    'Content-Type': rendererInfo.contentType
  };

  return context;
}

module.exports.render = render;