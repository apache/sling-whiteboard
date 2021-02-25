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
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.contentmapper.api.ContentMapper;
import org.apache.sling.contentmapper.api.MappingTarget;
import org.apache.sling.experimental.typesystem.Type;
import org.apache.sling.experimental.typesystem.service.TypeSystem;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.contentmapper.api.AnnotationNames.VISIT_CONTENT;
import static org.apache.sling.contentmapper.api.AnnotationNames.DOCUMENT_ROOT;
import static org.apache.sling.contentmapper.api.AnnotationNames.VISIT_CONTENT_RESOURCE_NAME_PATTERN;

import static org.apache.sling.contentmapper.api.AnnotationNames.DEREFERENCE_BY_PATH;

@Component(service = ContentMapper.class, property = { ContentMapper.ROLE + "=content" })
public class ContentContentMapper implements ContentMapper {

    private final PropertiesMapper propertiesMapper = new PropertiesMapper();
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private TypeSystem typeSystem;

    @Override
    public void map(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, UrlBuilder urlb) {
        final Type t = typeSystem.getType(r);
        final ContentPropertiesSelector hints = new ContentPropertiesSelector(t);
        dest.addValue("path", r.getPath());
        mapResource(r, dest, urlb, t, hints, TypeUtil.hasAnnotation(t, DOCUMENT_ROOT));
    }

    private void mapResource(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, 
        UrlBuilder urlb, Type parentType, ContentPropertiesSelector selector, boolean recurse) {

        log.debug("Mapping Resource {} as {}", r.getPath(), r.getResourceType());
        propertiesMapper.mapProperties(dest, r, selector);

        // Dereference by path if specified
        // TODO detect cycles which might lead to infinite loops
        final String derefPathPropertyName = TypeUtil.getAnnotationValue(parentType, DEREFERENCE_BY_PATH);
        if(derefPathPropertyName != null) {
            log.debug("Dereferencing {} on {}", r.getPath(), derefPathPropertyName);
            final ValueMap vm = r.adaptTo(ValueMap.class);
            final String derefPath = vm == null ? null : vm.get(derefPathPropertyName, String.class);
            if(derefPath != null) {
                final Resource dereferenced = r.getResourceResolver().getResource(derefPath);
                if(dereferenced != null) {
                    final MappingTarget.TargetNode derefNode = dest.addChild("dereferenced_by_" + derefPathPropertyName);
                    mapResource(dereferenced, derefNode, urlb, parentType, selector, recurse);
                }
            }
        }

        // TODO detect too much recursion?
        if(recurse) {
            log.debug("Recursing into {}", r.getPath());
            final String namePattern = TypeUtil.getAnnotationValue(parentType, VISIT_CONTENT_RESOURCE_NAME_PATTERN);
            for(Resource child : r.getChildren()) {
                if(namePattern != null && !child.getName().equals(namePattern)) {
                    continue;
                }
                final Type childType = typeSystem.getType(child);
                if(TypeUtil.hasAnnotation(childType, VISIT_CONTENT)) {
                    final MappingTarget.TargetNode childDest = dest.addChild(child.getName());
                    mapResource(child, childDest, urlb, childType, selector, true);
                }
            }
        } else if(log.isDebugEnabled()) {
            log.debug("NOT recursing into {}", r.getPath());
        }
    }
}