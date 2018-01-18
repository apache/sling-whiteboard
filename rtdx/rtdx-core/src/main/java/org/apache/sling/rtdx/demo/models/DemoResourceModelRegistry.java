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

package org.apache.sling.rtdx.demo.models;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import org.apache.sling.rtdx.api.*;
import java.util.Map;
import org.osgi.service.component.annotations.Component;

/** Temporary ResourceModelRegistry that provides a set of
 *  demo ResourceModels */
@Component(service = ResourceModelRegistry.class,
    property = {
            "service.description=RTD-X Demo Model Registry",
            "service.vendor=The Apache Software Foundation"
    })
public class DemoResourceModelRegistry implements ResourceModelRegistry {

    private final Map<String, ResourceModel> models = new HashMap<>();
    
    public DemoResourceModelRegistry() {
        addModel(ResourceModel.BUILDER()
                .withName("rtdx/root")
                .withDescription("RTD-X Root")
                .withPostHereResourceType("rtdx/blog/home")
                .build()
        );
        
        addModel(ResourceModel.BUILDER()
                .withName("rtdx/blog/home")
                .withDescription("Homepage of a Blog")
                .withPostHereResourceType("rtdx/blog/post")
                .withPostHereResourceType("rtdx/blog/folder")
                .withProperty(new ResourceProperty("title", "Title", true, "rtdx:string"))
                .build()
        );
        
        addModel(ResourceModel.BUILDER()
                .withName("rtdx/blog/folder")
                .withDescription("A Folder in a Blog")
                .withPostHereResourceType("rtdx/blog/post")
                .withProperty(new ResourceProperty("title", "Title", true, "rtdx:string"))
                .build()
        );
        
        addModel(ResourceModel.BUILDER()
                .withName("rtdx/blog/post")
                .withDescription("A Blog Post")
                .withProperty(new ResourceProperty("title", "Title", true, "rtdx:string"))
                .withProperty(new ResourceProperty("text", "Text", true, "rtdx:richtext"))
                .build()
        );
    }
    
    private void addModel(ResourceModel m) {
        models.put(m.getName(), m);
    }
    
    @Override
    public ResourceModel getModel(String resourceType) {
        return models.get(resourceType);
    }

    @Override
    public Collection<ResourceModel> getModels() {
        return Collections.unmodifiableCollection(models.values());
    }
}