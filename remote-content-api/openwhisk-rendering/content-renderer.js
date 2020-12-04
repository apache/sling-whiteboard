/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Note that I'm still not a JavaScript guru...but this is
// just prototype/demo code ;-)

var Handlebars = require('handlebars')

class HandlebarsRenderer {
    constructor(templateSrc) {
        this.template = Handlebars.compile(templateSrc);
    }

    render(content) { 
        return this.template(content) 
    }
}

var renderers = {}

var DEFAULT_RENDERER = new HandlebarsRenderer('\
    <h3>Default renderer</h3>\
    <b>Path</b>:{{path}}<br>\
    <b>sling:resourceType</b>:{{sling:resourceType}}<br>\
    ')

renderers["samples/article"] = new HandlebarsRenderer('\
      <h3>Articles renderer</h3>\
      <h4>{{title}}</h4>\
      <div><b>tags</b>:{{tags}}</div>\
      <blockquote>{{{text}}}</blockquote>\
    ')

renderers["samples/section"] = new HandlebarsRenderer('\
    <h3>Articles section renderer</h3>\
    This is the <b>{{name}}</b> section.\
    ')

class WkndPageRenderer extends HandlebarsRenderer {
    constructor(templateSrc) {
        super(templateSrc)
    }

    collectResourceTypes(resource, targetSet) {
        for(const key in resource) {
            const prop = resource[key]
            if("sling:resourceType" == key) {
                targetSet.add(`${prop} ${renderers[prop] ? " (renderer provided)": ""}`)
            } else if(typeof prop == "object") {
                this.collectResourceTypes(prop, targetSet)
            }
        }
    }

    render(content) { 
        content.rendererInfo = { resourceTypes:new Set() }
        this.collectResourceTypes(content, content.rendererInfo.resourceTypes)
        return super.render(content)
    }
}
renderers["wknd/components/page"] = new WkndPageRenderer('\
    <h3>WKND page renderer</h3>\n\
    <div>tags: {{jcr:content.cq:tags}}</div>\n\
    <h4>We will need renderers for the following resource types, found in the page content:</h4>\n\
    <ul>\n\
      {{#each rendererInfo.resourceTypes}}\n\
      <li>{{this}}</li>\n\
      {{/each}}\
    </ul></div>\n\
    ');

class ContentRenderer {
    info() {
        return `The ContentRenderer only supports a limited set of sling:resourceType values: ${Object.keys(renderers)}`
    }

    getResourceType(content, supertype) {
        var result = null;
        var key = supertype ? "sling:resourceSuperType" : "sling:resourceType"
        result = content[key]
        if(!result && content["jcr:content"]) {
            result = content["jcr:content"][key]
        }
        return result;
    }

    render(content) {
        var renderer = renderers[this.getResourceType(content, false)]
        if(!renderer) {
            renderer = renderers[this.getResourceType(content, true)]
        }
        if(!renderer) {
            renderer = DEFAULT_RENDERER
        }
        return renderer.render(content)
    }
}

module.exports = ContentRenderer;