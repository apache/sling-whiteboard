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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.sling.rtdx.api.*;

/** Demonstration ResourceModels - those should rather be provided
 *  by YAML or other structured text files, ready dynamically
 *  from bundle resources or the Sling content repository
 */
class DemoModels {

    static Collection<ResourceModel> getModels() {
        final List<ResourceModel> list = new ArrayList<>();
        
        list.add(ResourceModel.BUILDER()
                .withName("rtdx/root")
                .withDescription("RTD-X Root")
                .withPostHereResourceType("rtdx/blog/home")
                .build()
        );
        
        list.add(ResourceModel.BUILDER()
                .withName("rtdx/blog/home")
                .withDescription("Homepage of a Blog")
                .withPostHereResourceType("rtdx/blog/post")
                .withPostHereResourceType("rtdx/blog/folder")
                .withProperty(new ResourceProperty("title", "Title", true, "rtdx:string"))
                .build()
        );
        
        list.add(ResourceModel.BUILDER()
                .withName("rtdx/blog/folder")
                .withDescription("Folder for Blog Posts")
                .withPostHereResourceType("rtdx/blog/post")
                .withPostHereResourceType("rtdx/blog/folder")
                .withProperty(new ResourceProperty("title", "Title", true, "rtdx:string"))
                .build()
        );
        
        list.add(ResourceModel.BUILDER()
                .withName("rtdx/blog/post")
                .withDescription("Blog Post")
                .withProperty(new ResourceProperty("title", "Title", true, "rtdx:string"))
                .withProperty(new ResourceProperty("text", "Text", true, "rtdx:richtext"))
                .build()
        );
        return Collections.unmodifiableCollection(list);
    }
}