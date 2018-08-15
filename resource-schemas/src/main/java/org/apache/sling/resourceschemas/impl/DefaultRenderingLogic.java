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

package org.apache.sling.resourceschemas.impl;

import org.apache.sling.resourceschemas.api.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.resourceschemas.api.ResourceRenderer.NavigationItem;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = RenderingLogic.class,
    property = {
            "service.description=Sling Resource Schemas - Default Rendering Logic",
            "service.vendor=The Apache Software Foundation"
    })
public class DefaultRenderingLogic implements RenderingLogic {

    private static NavigationItem [] NI_ARRAY = {};
    private static ResourceAction [] ACT_ARRAY = {};
    
    // TODO: pagination would be better...
    public static final int MAX_NAV_CHILDREN = 20;
    
    @Reference
    private ResourceSchemaRegistry registry;
    
    @Override
    public void render(Resource r, ResourceSchema s, ResourceRenderer rr) throws IOException {
        
        rr.renderPrefix(r, s);
        
        final List<NavigationItem> nav = new ArrayList<>();
        
        // Navigation
        if(r.getParent() != null) {
            final Resource parent = r.getParent();
            nav.add(new NavigationItem(ResourceRenderer.NavigationType.PARENT, parent, registry.getSchema(parent.getResourceType())));
        }
        
        int availableLinks = MAX_NAV_CHILDREN;
        final Iterator<Resource> it = r.getResourceResolver().listChildren(r);
        while(it.hasNext()) {
            if(availableLinks-- <= 0) {
                // TODO do something about it
                break;
            }
            final Resource child = it.next();
            nav.add(new NavigationItem(ResourceRenderer.NavigationType.CHILD, child, registry.getSchema(child.getResourceType())));
        }
        
        rr.renderNavigationItems(r, nav.toArray(NI_ARRAY));
        rr.renderContent(r, s);
        rr.renderActions(r, s.getActions().toArray(ACT_ARRAY));
        rr.renderSuffix(r, s);
    }
}