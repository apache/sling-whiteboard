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

import java.util.Arrays;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contentmapper.api.ContentMapper;
import org.apache.sling.contentmapper.api.MappingTarget;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

@Component(service = ContentMapper.class, property = { ContentMapper.ROLE + "=content" })
public class ContentContentMapper implements ContentMapper {

    @Override
    public void map(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, UrlBuilder urlb) {
        // TODO use the type system to decide which properties to render here,
        // using a renderInContent annotation on the property?
        dest
            .addValue("source", getClass().getName())
            .addValue("path", r.getPath())
        ;
        addValues(dest, r);

        // TODO use the type system to decide whether to recurse under this Resource
        // to render more content
    }

    private static void addValues(MappingTarget.TargetNode dest, Resource r) {
        final ValueMap vm = r.adaptTo(ValueMap.class);
        if(vm != null) {
            for(Map.Entry<String, Object> e : vm.entrySet()) {
                final Object value = e.getValue();
                if(value instanceof Object[]) {
                    dest.addValue(e.getKey(), Arrays.asList((Object[])value));
                } else {
                    dest.addValue(e.getKey(), String.valueOf(value));
                }
            }
        }
    }
}