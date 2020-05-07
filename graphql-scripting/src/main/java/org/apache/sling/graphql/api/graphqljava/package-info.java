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

 /**
  * This package contains APIs which are specific to the
  * Sling GraphQL core implementation that's based on the
  * com.graphql-java:graphql-java library.
  *
  * If we later want our API to be independent of that, we
  * can deprecate this package in favor of generic implementations
  * that would then be in the parent package.
  */
@Version("1.0.0")
package org.apache.sling.graphql.api.graphqljava;
import org.osgi.annotation.versioning.Version;