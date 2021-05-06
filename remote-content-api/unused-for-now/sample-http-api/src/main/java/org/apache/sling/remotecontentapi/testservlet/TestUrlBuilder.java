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

package org.apache.sling.remotecontentapi.testservlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.documentmapper.api.DocumentMapper;

class TestUrlBuilder implements DocumentMapper.UrlBuilder {
    private SlingHttpServletRequest request;

    TestUrlBuilder(SlingHttpServletRequest request) {
        this.request = request;
    }

    public String pathToUrlNoExtension(String path) {
        return String.format(
            "%s://%s:%d%s",
            request.getScheme(),
            request.getServerName(),
            request.getServerPort(),
            path
        );
    }

    @Override
    public String pathToUrl(String path) {
        return String.format(
            "%s.%s.%s",
            pathToUrlNoExtension(path),
            request.getRequestPathInfo().getSelectorString(),
            request.getRequestPathInfo().getExtension()
        );
    }
}