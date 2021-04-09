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
package org.apache.sling.mdresource.impl;

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;

/**
 * Resource wrapper for supporting markdown handling
 */
public class MarkdownResourceWrapper extends ResourceWrapper {

    private final ResourceConfiguration config;

    private volatile ValueMap valueMap;

    public MarkdownResourceWrapper(final Resource original, final ResourceConfiguration config) {
        super(original);
        this.config = config;
    }

    @Override
    public String getResourceType() {
        return this.config.resourceType;
    }

    @Override
    public String getResourceSuperType() {
        return getResource().getResourceType();
    }

    @Override
    public ValueMap getValueMap() {
        if (valueMap == null) {
            valueMap = ResourceUtils.newValueMap(this.config, this, this.getResource().getValueMap());
        }

        return valueMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T adaptTo(Class<T> type) {
        if ( type == ValueMap.class || type == Map.class ) {
            return (T) getValueMap();
        }

        return super.adaptTo(type);
    }
}
