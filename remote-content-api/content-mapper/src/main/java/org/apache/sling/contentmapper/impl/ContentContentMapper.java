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
import org.apache.sling.experimental.typesystem.Type;
import org.apache.sling.experimental.typesystem.service.TypeSystem;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import static org.apache.sling.contentmapper.api.AnnotationNames.VISIT_CONTENT;
import static org.apache.sling.contentmapper.api.AnnotationNames.DOCUMENT_ROOT;
import static org.apache.sling.contentmapper.api.AnnotationNames.VISIT_CONTENT_RESOURCE_NAME_PATTERN;

@Component(service = ContentMapper.class, property = { ContentMapper.ROLE + "=content" })
public class ContentContentMapper implements ContentMapper {

    @Reference
    private TypeSystem typeSystem;

    @Override
    public void map(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, UrlBuilder urlb) {
        final Type t = typeSystem.getType(r);
        final RenderingHints hints = new RenderingHints(t);
        dest.addValue("path", r.getPath());
        mapResource(r, dest, urlb, t, hints, TypeUtil.hasAnnotation(t, DOCUMENT_ROOT));
    }

    private void mapResource(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, 
        UrlBuilder urlb, Type parentType, RenderingHints hints, boolean recurse) {
        addValues(dest, r, hints);

        // TODO detect too much recursion?
        if(recurse) {
            final String namePattern = TypeUtil.getAnnotationValue(parentType, VISIT_CONTENT_RESOURCE_NAME_PATTERN);
            for(Resource child : r.getChildren()) {
                if(namePattern != null && !child.getName().equals(namePattern)) {
                    continue;
                }
                final Type childType = typeSystem.getType(child);
                if(TypeUtil.hasAnnotation(childType, VISIT_CONTENT)) {
                    final MappingTarget.TargetNode childDest = dest.addChild(child.getName());
                    mapResource(child, childDest, urlb, childType, hints, true);
                }
            }
        }
    }

    private static void addValues(MappingTarget.TargetNode dest, Resource r, RenderingHints hints) {
        final ValueMap vm = r.adaptTo(ValueMap.class);
        if(vm != null) {
            for(Map.Entry<String, Object> e : vm.entrySet()) {
                if(!hints.renderProperty(e.getKey())) {
                    continue;
                }
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