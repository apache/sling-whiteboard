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

package org.apache.sling.jsonstore.internal.impl;

import java.util.regex.Pattern;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.jsonstore.internal.api.JsonStoreConstants;
import org.apache.sling.jsonstore.internal.api.ResourceBehaviorProvider;
import org.osgi.service.component.annotations.Component;

@Component(service=ResourceBehaviorProvider.class)
public class ResourceBehaviorProviderImpl implements ResourceBehaviorProvider {

    // TODO all path patterns should be configurable in a central place
    private final static Pattern AUTHORING_CONTENT_PATTERN = Pattern.compile("/content/sites/([^/]+)/branches/authoring/content/(.*)");
    private final static Pattern AUTHORING_ELEMENTS_PATTERN = Pattern.compile("/content/sites/([^/]+)/branches/authoring/elements/(.*)");
    private final static Pattern SCHEMA_PATTERN = Pattern.compile("/content/sites/([^/]+)/schema/(.*)");
    private final static Pattern BRANCHES_PATTERN = Pattern.compile("/content/sites/([^/]+)/branches/(.*)");

    @Override
    public ResourceBehavior getBehavior(Resource r) {
        final String path = r.getPath();
        if(AUTHORING_CONTENT_PATTERN.matcher(path).matches()) {
            return new ResourceBehavior(JsonStoreConstants.CONTENT_RESOURCE_TYPE, "GET", "POST");
        } else if(AUTHORING_ELEMENTS_PATTERN.matcher(path).matches()) {
            return new ResourceBehavior(JsonStoreConstants.ELEMENTS_RESOURCE_TYPE, "GET", "POST");
        } else if(SCHEMA_PATTERN.matcher(path).matches()) {
            return new ResourceBehavior(JsonStoreConstants.SCHEMA_RESOURCE_TYPE, "GET", "POST");
        } else if(BRANCHES_PATTERN.matcher(path).matches()) {
            return new ResourceBehavior(null, "GET");
        }
        return null;
    }
}