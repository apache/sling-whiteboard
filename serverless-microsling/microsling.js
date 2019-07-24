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

const { resolveContent } = require('./lib/resolve-content.js');
const { render } = require('./lib/render.js');

function main (params) {
  const { debug } = params;
  const context = {
    request : {
      path: params.__ow_path,
    },
    response: {},
    content: {}
  };

  return new Promise(function (resolvePromise) {
    if(debug) console.log(`start: ${JSON.stringify(context, 2, null)}`);
    resolveContent(context)
    .then(context => {
      if(debug) console.log(`pre-render: ${JSON.stringify(context, 2, null)}`);
      return render(context);
    })
    .then(context => {
      if(debug) console.log(`pre-resolve: ${JSON.stringify(context, 2, null)}`);
      return resolvePromise(context.response);
    })
    .catch(e => {
      if(e.httpStatus) {
        return resolvePromise({ status: e.httpStatus, body: e.message});
      } else {
        throw e;
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
    // eslint-disable-next-line no-undef
    __ow_path: process.argv[2],
    __ow_method: 'get',
    debug: false
  });
}

module.exports.main = main