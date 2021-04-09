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
var request = require('request')
var ContentRenderer = require('./content-renderer.js')
var Handlebars = require('handlebars')
var ORIGIN_SUFFIX = '.rakam.json'
var contentRenderer = new ContentRenderer();

var templateSource = '\
  <html>\
  <head>\
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@exampledev/new.css@1.1.2/new.min.css">\
  <style type="text/css">\
    body{\
      max-width: 9999px;\
    }\
    .debug {\
      font-size:80%;\
      color:gray;\
    }\
    ul {\
      list-style-type: none;\
    }\
    .navigation li,\
    .content li {\
      margin:0;\
      padding:0;\
    }\
    .content,\
    .raw pre,\
    .navigation {\
      margin-left:2em;\
    }\
    .navigation {\
      background-color: #CCFFFF;\
    }\
    .content {\
      background-color: #FFFFCC;\
    }\
    .raw pre {\
      background-color: #EEE;\
    }\
    .nested {\
      margin-left:1em;\
    }\
  </style>\
  </head>\
  <body> \
  \
  <h1>Sling Remote Content API :: HTTP proxying renderer</h1> \
  <div class="debug">\
  This is proxying content from a Sling instance at <a href="{{proxyInfo.origin}}">{{proxyInfo.origin}}</a>\
  where the document-oriented <a href="https://github.com/apache/sling-whiteboard/tree/master/remote-content-api">Sling Remote Content API</a> must be installed.\
  <br>Source code is found in the <a href="https://github.com/apache/sling-whiteboard/tree/master/remote-content-api/openwhisk-rendering">openwhisk-rendering</a> module there<br>\
  {{proxyInfo.contentRendererInfo}}<br>\
  </div>\
  \
  <h2>Navigation</h2>\
  <div class="navigation">\
  \
  Parent\
  <ul>\
  <li><a href="{{navigation.parent}}">{{navigation.parent}}</a></li>\
  </ul>\
  Children\
  <ul>\
    {{#each navigation.children}} \
    <li><a href="{{this.url}}">{{this.path}}</a></li>\
    {{/each}} \
  </ul>\
  </div>\
  \
  <h2>Rendered Content</h2>\
  <div class="content">\
  {{{renderedContent}}}\
  \
  </div>\
  <h2>Raw Content</h2>\
  Acquired from <a href="{{proxyInfo.sourceURL}}">{{proxyInfo.sourceURL}}</a>\
  <div class="raw">\
  <pre>{{rawJSON}}</pre>\
  </div>\
  </body></html>'

var template = Handlebars.compile(templateSource);
var origin = "ORIGIN_NOT_SET"

// TODO get this from the environment
var myRootURL = "ROOT_URL_NOT_SET"

function convertUrl(url) {
  return url.replace(origin, myRootURL).replace(ORIGIN_SUFFIX, "")
}

function convertUrls(navigation) {
  navigation.self = convertUrl(navigation.self)
  navigation.parent = convertUrl(navigation.parent)
  for(const i in navigation.children) {
    var c = navigation.children[i]
    c.url = convertUrl(c.url)
  }
}

function main (params) {

  var path = params.__ow_path
  if(path == "") {
    path = "/"
  }
  origin = params.ORIGIN
  myRootURL = params.ROOT_URL
  var sourceURL = `${origin}${path}${ORIGIN_SUFFIX}`
  console.log(`Proxying to ${sourceURL}, converting links to point to ${myRootURL}`)
  
  var options = {
    url: sourceURL,
    json: true
  }

  return new Promise(function (resolve, reject) {
    request(options, function (err, resp) {
        
      if (err) {
        // request failed  
        console.log(err)
        return resolve({ statusCode:500, body:err})
      }
      
      if(resp.statusCode != 200) {
        // service error 
        console.log(`service error ${resp.statusCode}: ${resp.body.error}`)   
        return resolve({ statusCode:resp.statusCode, body:resp.body.error})
      }

      var data = resp.body
      convertUrls(data.navigation)
      data.rawJSON = JSON.stringify(resp.body, null, 2)
      data.renderedContent = contentRenderer.render(data.content)
      data.proxyInfo = {
        origin : origin,
        sourceURL : sourceURL,
        contentRendererInfo : contentRenderer.info()
      }
      var html = template(data);
      
      console.log(`Returning ${html}`)
      return resolve({ body: html, headers:{ 'Content-Type': 'text/html'}}) 
    })
  }) 
}

// This is for command-line testing
// like node -e "require('./http-rendering-proxy.js').main({ ORIGIN:'http://localhost:8080', __ow_path : '/content' })"
module.exports.main = main