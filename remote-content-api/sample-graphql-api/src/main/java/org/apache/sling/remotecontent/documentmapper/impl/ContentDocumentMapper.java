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

package org.apache.sling.remotecontent.documentmapper.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.remotecontent.documentmapper.api.Annotations;
import org.apache.sling.remotecontent.documentmapper.api.AnnotationsRegistry;
import org.apache.sling.remotecontent.documentmapper.api.DocumentMapper;
import org.apache.sling.remotecontent.documentmapper.api.MappingTarget;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = DocumentMapper.class, property = { DocumentMapper.ROLE + "=content" })
public class ContentDocumentMapper implements DocumentMapper {

    private final PropertiesMapper propertiesMapper = new PropertiesMapper();
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private AnnotationsRegistry annotationsRegistry;

    @Override
    public void map(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, UrlBuilder urlb) {
        final String resourceType = r.getResourceType();
        final Annotations annot = annotationsRegistry.getAnnotations(resourceType);
        dest.addValue("path", r.getPath());
        log.debug("Top level Resource map {} as {}: {}", r.getPath(), r.getResourceType(), annot);
        mapResource(r, dest, urlb, resourceType, annot, annot.isDocumentRoot());
    }

    private void mapResource(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, 
        UrlBuilder urlb, String documentResourceType, Annotations documentAnnot, boolean recurse) {

        log.debug("Mapping Resource {} as {}: {}", r.getPath(), r.getResourceType(), documentAnnot);
        propertiesMapper.mapProperties(dest, r, documentAnnot);
        final Annotations thisAnnot = annotationsRegistry.getAnnotations(r.getResourceType());

        // Dereference by path if specified
        // TODO detect cycles which might lead to infinite loops
        thisAnnot.dereferenceByPathPropertyNames().forEach(derefPathPropertyName -> {
            log.debug("Dereferencing {} on {}", r.getPath(), derefPathPropertyName);
            final ValueMap vm = r.adaptTo(ValueMap.class);
            final String derefPath = vm == null ? null : vm.get(derefPathPropertyName, String.class);
            if(derefPath != null) {
                final Resource dereferenced = r.getResourceResolver().getResource(derefPath);
                if(dereferenced != null) {
                    final MappingTarget.TargetNode derefNode = dest.addChild("dereferenced_by_" + derefPathPropertyName);
                    mapResource(dereferenced, derefNode, urlb, documentResourceType, documentAnnot, recurse);
                }
            }
        });

        // TODO detect too much recursion?
        if(recurse) {
            log.debug("Recursing into {}", r.getPath());
            for(Resource child : r.getChildren()) {
                final boolean visit = thisAnnot.visitChildResource(child.getName());
                log.debug("child resource {} visit decision {}", child.getName(), visit);
                if(!visit) {
                    continue;
                }
                final String childResourceType = child.getResourceType();
                if(annotationsRegistry.getAnnotations(childResourceType).visitContent()) {
                    final MappingTarget.TargetNode childDest = dest.addChild(child.getName());
                    mapResource(child, childDest, urlb, childResourceType, documentAnnot, true);
                }
            }
        } else if(log.isDebugEnabled()) {
            log.debug("NOT recursing into {}", r.getPath());
        }
    }
}