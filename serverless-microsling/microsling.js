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

const { resolveContent } = require('./lib/resolve-content.js');

function main (params) {
  const context = {
    path: params.__ow_path,
    content: {},
  };

  return new Promise(function (resolve, reject) {
    resolveContent(context)
    .then(context => {
      return resolve({body: context});
    })
    .catch(e => {
      if(e.httpStatus) {
        return resolve({ status: e.httpStatus, body: e.message});
      } else {
        return resolve({ status: 500, body: e});
      }
    })
  })
}

const shellExec= async (input) => {
  main(input)
  .then(result => {
    console.log(JSON.stringify(result, 2, null));
  });
};

if (require.main === module) {
  shellExec({
    __ow_path: process.argv[2],
    __ow_method: 'get',
  });
}

module.exports.main = main