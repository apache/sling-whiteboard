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

package org.apache.sling.jsonstore.impl;

import javax.servlet.http.HttpServletRequest;

import static org.apache.sling.jsonstore.api.JsonStoreConstants.*;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wrap non-existing resources from our jsonstore subtree
 *  so that the correct servlets are used to create them
 */
@Component(service=ResourceDecorator.class)
public class JsonStoreResourceDecorator implements ResourceDecorator {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public @Nullable Resource decorate(@NotNull Resource resource) {
        if(ResourceUtil.isNonExistingResource(resource) && resource.getPath().startsWith(STORE_ROOT_PATH)) {
            final String wrappedResourceType = getWrappedResourceType(resource);
            log.debug("Wrapping resource {} with resource type {}", resource.getPath(), wrappedResourceType);
            return new WrappedResource(resource);
        } else {
            return resource;
        }
    }

    String getWrappedResourceType(Resource r) {
        return JSON_BLOB_RESOURCE_TYPE;
    }

    @Override
    public @Nullable Resource decorate(@NotNull Resource resource, @NotNull HttpServletRequest request) {
        return this.decorate(resource);
    }
}
