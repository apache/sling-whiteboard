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

 const openwhisk = require('openwhisk');

const getAnnotation = (act, key) => {
  const annotation = act.annotations.find(ann => key == ann.key);
  return annotation ? annotation.value : undefined;
}

const getAction = async (resourceType, extension) => {
  return new Promise(resolve => {
    var ow = openwhisk();
    ow.actions.list()
    .then(actions => {
      const act = actions.find(act => {
        return resourceType == getAnnotation(act, 'sling:resourceType') && extension == getAnnotation(act, 'sling:extensions')
      })
      resolve(act);
    })
    .catch(e => {
      throw e;
    })
  })
};

const render = (resource, action) => {
  const name = action.name;
  const blocking = true, result = true
  const params = {
    resource: resource
  }
  var ow = openwhisk();
  return ow.actions.invoke({name, blocking, result, params});
};

 const renderer = {
  contentType: 'text/html',
  getRendererInfo : async (resourceType, extension) => { 
    return getAction(resourceType, extension)
  },
  render : (resource, action) => {
    return render(resource, action);
  },
}

// For testing as a standalone OpenWhisk action
function main () {
  return new Promise(async resolve => {
    const resource = {
      title: 'cmdline title test',
      body: 'cmdline body test',
    }
    resolve(render(resource, getAction('microsling/somedoc', 'html')));
  });
}

module.exports.openWhiskRenderer = renderer;
module.exports.main = main;