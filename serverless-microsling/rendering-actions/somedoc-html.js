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

function main (params) {
  const { resource } = params;
  const markup = `<html>
  <head>
  <title>${resource.title}</title>
  <style type="text/css">
  body {
    background-color:  #fcf3cf;
  }
  h1 {
    color: blue;
  }
  </style>
  </head>
  <body>
  <h1>
      ${resource.title}, with a custom rendering!
  </h1>
  <div>${resource.body}</div>
  <div><em>This is the somedoc-html rendering</em></div>
  </body>
  </html>
  `;
  return {output: markup};
}
 
if (require.main === module) {
  const input = {
    resource: {
      title: 'cmdline title',
      body: 'cmdline body'
    }
  };
  console.log(JSON.stringify(main(input), 2, null));
}
 
module.exports.main = main