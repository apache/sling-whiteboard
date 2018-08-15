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

package org.apache.sling.resourceschemas.api;

import java.io.IOException;
import org.apache.sling.api.resource.Resource;

public interface ResourceRenderer {
    
    enum NavigationType {
        PARENT,
        CHILD,
        SELF
    }
    
    class NavigationItem {
        public final NavigationType type;
        public final Resource resource;
        public final ResourceSchema schema;
        
        public NavigationItem(NavigationType t, Resource r, ResourceSchema s) {
            type = t;
            resource = r;
            schema = s;
        }
    }
    
    public void renderPrefix(Resource r, ResourceSchema s) throws IOException;
    public void renderSuffix(Resource r, ResourceSchema s) throws IOException;
    public void renderContent(Resource r, ResourceSchema s) throws IOException;
    public void renderNavigationItems(Resource r, NavigationItem ... item) throws IOException;
    public void renderActions(Resource r, ResourceAction ... action) throws IOException;
}