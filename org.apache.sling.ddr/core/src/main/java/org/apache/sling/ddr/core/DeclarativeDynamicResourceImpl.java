/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.ddr.core;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.ddr.api.DeclarativeDynamicResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.apache.sling.ddr.api.Constants.DDR_INDICATOR;
import static org.apache.sling.ddr.api.Constants.SLING_RESOURCE_SUPER_TYPE_PROPERTY;

public class DeclarativeDynamicResourceImpl
    extends SyntheticResource
    implements DeclarativeDynamicResource
{
    private static List<String> IGNORED_ATTRIBUTES = new ArrayList<>(Arrays.asList("jcr:created", SLING_RESOURCE_SUPER_TYPE_PROPERTY));

    private static final Logger log = LoggerFactory.getLogger(DeclarativeDynamicResourceImpl.class);

    /**
     * Creates a Resource that adds a Resource from a given location (source)
     * into a different location (target path)
     *
     * @param resourceResolver Resource Resolver to be used here
     * @param source Resource that provides the data
     * @param targetPath Path of the new, dynamic location of the resource
     * @return The Synthetic Resource that will provide the Dynamic Resource
     */
    public static DeclarativeDynamicResource createSyntheticFromResource(ResourceResolver resourceResolver, Resource source, String targetPath) {
        ValueMap properties = source.getValueMap();
        Map<String,String> parameters = new HashMap<>();
        String resourceSuperType = source.getResourceSuperType();
        for(Entry<String, Object> entry: properties.entrySet()) {
            if(!IGNORED_ATTRIBUTES.contains(entry.getKey())) {
                parameters.put(entry.getKey(), entry.getValue() + "");
            }
        }
        parameters.put(DDR_INDICATOR, "true");
        ResourceMetadata metadata = new ResourceMetadataWrapper();
        metadata.setParameterMap(parameters);
        metadata.setResolutionPath(targetPath);
        metadata.setResolutionPathInfo(targetPath);
        metadata.setCreationTime(System.currentTimeMillis());
        return new DeclarativeDynamicResourceImpl(
            resourceResolver,
            metadata,
            source.getResourceType(),
            resourceSuperType
        );
    }

    public DeclarativeDynamicResourceImpl(ResourceResolver resourceResolver, String path, String resourceType, String resourceSuperType) {
        super(resourceResolver, path, resourceType);
        this.resourceSuperType = resourceSuperType;
    }

    public DeclarativeDynamicResourceImpl(ResourceResolver resourceResolver, ResourceMetadata rm, String resourceType, String resourceSuperType) {
        super(resourceResolver, rm, resourceType);
        this.resourceSuperType = resourceSuperType;
    }

    private String resourceSuperType;

    @Override
    public String getResourceSuperType() {
        return resourceSuperType;
    }

    public static class ResourceMetadataWrapper extends ResourceMetadata {

        private boolean set = false;

        @Override
        public void setParameterMap(Map<String, String> parameterMap) {
            // Allow the map to be set only once as Sling tries to reset the map
            // with wrong values
            //AS TODO: Investigate why
            if(!set) {
                super.setParameterMap(parameterMap);
            }
            set = true;
        }

        @Override
        public void lock() {
            // Disable Locking as this leads to error
            //AS TODO: investigate why this is called and later Sling tries to change it
//            log.info("Resource Meta Data locked", new Exception("Here we are"));
//            super.lock();
        }
    }

    @Override
    public ValueMap getValueMap() {
        final Map<String, String> map = getResourceMetadata().getParameterMap();
        if(map.isEmpty()) {
            log.warn("Synthetic Resource: '{}' does not return a parameter map", getResourceMetadata().getResolutionPath());
        }
        Map<String,Object> parameters = new HashMap<String,Object>() {{
            putAll(map);
        }};
        // Do not add a Resource Super Type when it is null or empty
        if(resourceSuperType != null && !resourceSuperType.isEmpty()) {
            parameters.put(SLING_RESOURCE_SUPER_TYPE_PROPERTY, resourceSuperType);
        }
        log.info("Value Map for DC: '{}': '{}'", getName(), parameters);
        return new ValueMapDecorator(parameters);
    }
}
