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
package org.apache.sling.mvresource.impl;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.DeepReadModifiableValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.h2.mvstore.MVMap;

public class MvResource extends AbstractResource {

    private MvValueMap properties;
    private String path;
    private ResourceResolver resolver;

    public MvResource(ResourceResolver resolver, String path, MvValueMap properties) {
        this.resolver = resolver;
        this.properties = properties;
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getResourceType() {
        String type = (String) properties.get("sling:resourceType");
        if (type == null) {
            type = (String) properties.get("jcr:primaryType");
        }
        if (type == null) {
            if (properties.isEmpty()) {
                return "sling:Folder";
            }
        }
        return type;
    }

    @Override
    public String getResourceSuperType() {
        if (properties.isEmpty()) {
            return null;
        }
        return (String) properties.get("sling:resourceSuperType");
    }

    private ResourceMetadata metaData  = new ResourceMetadata();
    
    @Override
    public ResourceMetadata getResourceMetadata() {
        return metaData;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    public MvValueMap getMVMap() {
        return properties;
    }

    @Override
    public ValueMap getValueMap() {
        return this.properties;
    }

}
