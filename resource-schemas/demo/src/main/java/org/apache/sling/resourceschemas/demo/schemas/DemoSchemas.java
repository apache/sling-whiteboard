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

package org.apache.sling.resourceschemas.demo.schemas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.sling.resourceschemas.api.*;

/** Demonstration ResourceSchemas- those should rather be provided
 *  by YAML or other structured text files, ready dynamically
 *  from bundle resources or the Sling content repository
 */
class DemoSchemas {

    static Collection<ResourceSchema> getSchemas() {
        final List<ResourceSchema> list = new ArrayList<>();
        
        list.add(ResourceSchema.BUILDER()
                .withName(RT_ROOT)
                .withDescription("SRS Demo Root")
                .withAction(new CreateChildAction(RT_BLOG_HOME))
                .build()
        );
        
        list.add(ResourceSchema.BUILDER()
                .withName(RT_BLOG_HOME)
                .withDescription("Homepage of a Blog")
                .withAction(new CreateChildAction(RT_BLOG_POST))
                .withAction(new CreateChildAction(RT_BLOG_FOLDER))
                .withProperty(new ResourceProperty("title", "Title", true, "srs:string"))
                .build()
        );
        
        list.add(ResourceSchema.BUILDER()
                .withName(RT_BLOG_FOLDER)
                .withDescription("Folder for Blog Posts")
                .withAction(new CreateChildAction(RT_BLOG_POST))
                .withAction(new CreateChildAction(RT_BLOG_FOLDER))
                .withProperty(new ResourceProperty("title", "Title", true, "srs:string"))
                .build()
        );
        
        list.add(ResourceSchema.BUILDER()
                .withName(RT_BLOG_POST)
                .withDescription("Blog Post")
                .withProperty(new ResourceProperty("title", "Title", true, "srs:string"))
                .withProperty(new ResourceProperty("text", "Text", true, "srs:richtext"))
                .build()
        );
        return Collections.unmodifiableCollection(list);
    }
    
    static String RT_ROOT = "srs/demo/root";
    static String RT_BLOG_HOME = "srs/demo/blog/home";
    static String RT_BLOG_FOLDER = "srs/demo/blog/folder";
    static String RT_BLOG_POST = "srs/blog/post";
    
}