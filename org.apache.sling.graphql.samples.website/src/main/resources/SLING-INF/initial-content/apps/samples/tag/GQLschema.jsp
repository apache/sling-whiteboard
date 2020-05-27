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

<%--
Generating the schemas in JSP might not be the best way
but it works for these initial tests - we might create
a "passthrough" script engine, or one that extracts the
additional DataFetcher information that we need.
--%>

type Query {
  # Convert the current Resource to its valueMap
  # to be able to use the default graphql-java 
  # PropertiesDataFetcher on (most of) its values
  ## fetch:samples/tagQuery
  articlesByTag : [ArticleRef]
}

type ArticleRef {
  title: String
  tags: [String]
  path: String
}