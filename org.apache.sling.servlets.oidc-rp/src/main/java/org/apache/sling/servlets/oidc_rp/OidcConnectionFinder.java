/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets.oidc_rp;

import java.net.URI;
import java.util.Optional;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

// TODO - bad name
public interface OidcConnectionFinder {

    // flow would be :
    // 1. get oidc token
    // 1a. found → all good
    // 1b. not found → redirect to oidc entry point uri
    // 2. after callback, redirect back to same page (how, cookie?)
    // see https://github.com/panva/node-openid-client/issues/83
    Optional<String> getOidcToken(OidcConnection connection, ResourceResolver resolver);

    URI getOidcEntryPointUri(OidcConnection connection, SlingHttpServletRequest request, String redirectPath);
}
