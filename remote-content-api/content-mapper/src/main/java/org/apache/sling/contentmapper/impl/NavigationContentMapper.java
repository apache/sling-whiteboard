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

package org.apache.sling.contentmapper.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contentmapper.api.ContentMapper;
import org.apache.sling.contentmapper.api.MappingTarget;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

@Component(service = ContentMapper.class, property = { ContentMapper.ROLE + "=navigation" })
public class NavigationContentMapper implements ContentMapper {

    @Override
    public void map(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, UrlBuilder urlb) {
        dest.addValue("self", urlb.pathToUrl(r.getPath()));
        
        final Resource parent = r.getParent();
        if(parent != null) {
            dest.addValue("parent", urlb.pathToUrl(parent.getPath()));
        }

        final MappingTarget.TargetNode children = dest.addChild("children");
        for(Resource child : r.getChildren()) {
            children
                .addChild(child.getName())
                .addValue("url", urlb.pathToUrl(child.getPath()))
                .addValue("path", child.getPath())
            ;
        }
    }
}