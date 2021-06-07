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
package org.apache.sling.sitemap.common;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Consumers may implement this interface to override the default externalisation behaviour.
 */
@ConsumerType
public interface SitemapLinkExternalizer {

    /**
     * A default implementation of the {@link SitemapLinkExternalizer} which may be used as fallback.
     */
    SitemapLinkExternalizer DEFAULT = new SitemapLinkExternalizer() {
        @Override
        public @Nullable String externalize(SlingHttpServletRequest context, String uri) {
            return context.getResourceResolver().map(context, uri);
        }

        @Override
        public @Nullable String externalize(Resource resource) {
            return resource.getResourceResolver().map(resource.getPath());
        }
    };

    /**
     * Implementations must return an absolute url for the given path in the context of the given request.
     *
     * @param request
     * @param path
     * @return an absolute url
     */
    @Nullable
    String externalize(SlingHttpServletRequest request, String path);

    /**
     * Implementations must return an absolute url for the given resource.
     *
     * @param resource
     * @return an absolute url
     */
    @Nullable
    String externalize(Resource resource);

}
