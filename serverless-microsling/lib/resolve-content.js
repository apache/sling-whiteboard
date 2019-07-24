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

async function resolveContent(context) {
  if(!context.path.startsWith('/demo/')) {
    throw {
      httpStatus: 404,
      message: `path not found: ${context.path}`,
    };
  }
  context.content["resource"] = {
    path: context.path,
    resourceType: 'microsling/demo',
    content: {
      title: "This is a demo resource",
      body: "Here's the body of the demo resource"
    }
  }
  return context;
}

module.exports.resolveContent = resolveContent;