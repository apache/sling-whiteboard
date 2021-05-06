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
    public void map(@NotNull Resource originalResource, @NotNull MappingTarget.TargetNode dest, DocumentMapper.Options opt) {
        Annotations annot = annotationsRegistry.getAnnotations(originalResource.getResourceType());
        dest.addValue("path", originalResource.getPath());
        final String substPath = annot.childSubstitutePath();
        Resource r = originalResource;
        if(substPath != null) {
            r = r.getChild(substPath);
            if(r == null) {
                throw new RuntimeException("Child " + substPath + " of resource " + originalResource.getPath() + "not found");
            }
            annot = annotationsRegistry.getAnnotations(r.getResourceType());
        }
        log.debug("Top level Resource map {} as {}: {}", r.getPath(), r.getResourceType(), annot);
        mapResource(r, dest, opt, r.getResourceType(), annot, annot.isDocumentRoot());
    }

    private void mapResource(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, 
        DocumentMapper.Options opt, String documentResourceType, Annotations documentAnnot, boolean recurse) {

        final Annotations resourceAnnot = annotationsRegistry.getAnnotations(r.getResourceType());
        final MappingTarget.TargetNode debug = opt.debug ? dest.addChild("sling:dmap:debug") : null;
        if(debug != null) {
            debug.addValue("sling:dmap:path", r.getPath());
            debug.addValue("sling:dmap:resourceType", r.getResourceType());
            debug.addValue("sling:dmap:documentAnnot", documentAnnot.toString());
            debug.addValue("sling:dmap:resourceAnnot", resourceAnnot.toString());
        }
    
        for(String name : documentAnnot.excludeNodeNames()) {
            if(name.equals(r.getName())) {
                if(debug != null) {
                    debug.addValue("sling:dmap:excluded", documentAnnot.toString());
                }
                log.debug("Resource {} excluded by node name ({})", r.getPath(), documentAnnot);
                return;
            }
        }

        log.debug("Mapping Resource {} as {}: {}", r.getPath(), r.getResourceType(), documentAnnot);
        propertiesMapper.mapProperties(dest, r, documentAnnot);

        // Resolve by path if specified
        // TODO detect cycles which might lead to infinite loops
        resourceAnnot.resolveByPathPropertyNames().forEach(derefPathPropertyName -> {
            log.debug("Resolving by path {} on {}", r.getPath(), derefPathPropertyName);
            final ValueMap vm = r.adaptTo(ValueMap.class);
            final String derefPath = vm == null ? null : vm.get(derefPathPropertyName, String.class);
            if(derefPath != null) {
                final Resource dereferenced = r.getResourceResolver().getResource(derefPath);
                if(dereferenced != null) {
                    final MappingTarget.TargetNode derefNode = dest.addChild("sling:dmap:resolved");
                    derefNode.addValue("sling:dmap:resolvedFrom", derefPathPropertyName);
                    derefNode.addValue("sling:dmap:resolvePath", derefPath);
                    mapResource(dereferenced, derefNode, opt, documentResourceType, documentAnnot, recurse);
                } else if(debug != null) {
                    debug.addValue("Resolve by path " + derefPathPropertyName, "not found:" + derefPath);
                }
            }
        });

        // TODO detect too much recursion?
        if(recurse) {
            log.debug("Recursing into {}", r.getPath());
            for(Resource child : r.getChildren()) {
                final boolean visit = resourceAnnot.visitChildResource(child.getName());
                log.debug("child resource {} visit decision {}", child.getName(), visit);
                if(!visit) {
                    continue;
                }
                final String childResourceType = child.getResourceType();
                if(annotationsRegistry.getAnnotations(childResourceType).visitContent()) {
                    final MappingTarget.TargetNode childDest = dest.addChild(child.getName());
                    mapResource(child, childDest, opt, childResourceType, documentAnnot, true);
                }
            }
        } else if(log.isDebugEnabled()) {
            log.debug("NOT recursing into {}", r.getPath());
        }
    }
}