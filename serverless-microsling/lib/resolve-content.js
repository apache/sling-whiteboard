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

const fs = require('fs');
const localFileSuffix = '.json';

async function resolveContent(context) {
  const parts = context.request.path.split('.');
  context.request.extension = parts.pop();
  context.request.resourcePath = parts.join('.');
  const filePath = `../content${context.request.resourcePath}${localFileSuffix}`;
  return new Promise(resolve => {
    try {
      fs.readFile(require.resolve(filePath), (err, text) => {
        context.content.resource = {
          path: context.request.path,
          content: JSON.parse(text)
        };
        return resolve(context);
    
      });
    } catch(e) {
      throw {
        httpStatus: 404,
        message: `path not found: ${context.request.path}`,
      };
    }
  });
}

module.exports.resolveContent = resolveContent;