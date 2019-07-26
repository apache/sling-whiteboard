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
  if(act != null) {
    const annotation = act.annotations.find(ann => key == ann.key);
    return annotation ? annotation.value : undefined;
  }
}

const getActionInfo = async (resourceType, extension) => {
  return new Promise(resolve => {
    var ow = openwhisk();
    ow.actions.list()
    .then(actions => {
      const act = actions.find(act => {
        const rtAnnotation = getAnnotation(act, 'sling:resourceType');
        return ('*' == rtAnnotation || resourceType ==  rtAnnotation)
          && extension == getAnnotation(act, 'sling:extensions');
      })
      let result;
      if(act) {
        result = {
          action: act,
          contentType: getAnnotation(act, 'sling:contentType'),
        };
      }
      resolve(result);
    })
    .catch(e => {
      throw e;
    })
  })
};

const renderWithAction = (resource, actionInfo) => {
  if(!actionInfo.action) {
    throw Error("No Action provided, cannot render");
  }
  const name = actionInfo.action.name;
  const blocking = true, result = true
  const params = {
    resource: resource
  }
  var ow = openwhisk();
  return ow.actions.invoke({name, blocking, result, params});
};

 const renderer = {
  getRendererInfo : async (resourceType, extension) => { 
    return getActionInfo(resourceType, extension);
  },
  render : (resource, action) => {
    return renderWithAction(resource, action);
  },
}

// For testing as a standalone OpenWhisk action
// (requires installing the action with -a provide-api-key true)
// or from the command line
function main () {
  return new Promise(async (resolve, reject) => {
    const resource = {
      title: 'cmdline title test',
      body: 'cmdline body test',
    }
    try {
      const actionInfo = await getActionInfo('microsling/somedoc', 'html');
      console.log(`actionInfo=${JSON.stringify(actionInfo, 2, null)}`);
      const rendered = await renderWithAction(resource, actionInfo);
      console.log(`rendered=${JSON.stringify(rendered, 2, null)}`);
      resolve(rendered);
    } catch(e) {
      reject(e);
    }
  });
}

// From the command line, __OW_API_HOST and __OW_API_KEY environment
// variables must be set
if (require.main === module) {
  main();
}

module.exports.openWhiskRenderer = renderer;
module.exports.main = main;