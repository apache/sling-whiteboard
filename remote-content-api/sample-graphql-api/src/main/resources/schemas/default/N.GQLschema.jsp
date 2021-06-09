<%-- 
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
--%>

<%-- N plane schema: Navigation --%>
scalar Object

type Query {
  folder(path: String) : Folder @fetcher(name:"samples/folder")
  folders(path: String, limit: Int, after: String) : FolderConnection @connection(for: "Folder") @fetcher(name:"samples/folders")
  document(path : String, selectors : [String], debug : Boolean) : Document @fetcher(name:"samples/document")
  documents(lang: String, query : String, debug: Boolean, limit: Int, after: String) : DocumentConnection @connection(for: "Document") @fetcher(name:"samples/documents")
}

type Mutation {
  command(lang: String, script: String) : CommandResult @fetcher(name:"samples/command")
}

type Folder {
  path : String
  header : DocumentHeader
}

type DocumentHeader {
  parent : String
  resourceType : String
  resourceSuperType : String
}

type Document {
  path : String
  header : DocumentHeader
  properties : Object
  body : Object
}

type CommandResult {
  success: Boolean
  output: String
  help: String
  links: [Link]
}

type Link {
  rel: String
  url: String
}