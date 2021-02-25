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

package org.apache.sling.remotecontentapi.typesystem;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.experimental.typesystem.Type;
import org.apache.sling.experimental.typesystem.service.TypeSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;

import static org.apache.sling.contentmapper.api.AnnotationNames.NAVIGABLE;
import static org.apache.sling.contentmapper.api.AnnotationNames.VISIT_CONTENT;
import static org.apache.sling.contentmapper.api.AnnotationNames.DOCUMENT_ROOT;
import static org.apache.sling.contentmapper.api.AnnotationNames.VISIT_CONTENT_RESOURCE_NAME_PATTERN;
import static org.apache.sling.contentmapper.api.AnnotationNames.CONTENT_EXCLUDE_PROPERTY_REGEXP;
import static org.apache.sling.contentmapper.api.AnnotationNames.CONTENT_INCLUDE_PROPERTY_REGEXP;
import static org.apache.sling.contentmapper.api.AnnotationNames.NAVIGATION_PROPERTIES_LIST;
import static org.apache.sling.contentmapper.api.AnnotationNames.DEREFERENCE_BY_PATH;

/** Temporary hardcoded type system for this prototype, until we
 *  have the actual type system
 */

@Component(service=TypeSystem.class)
public class HardcodedTypeSystem implements TypeSystem {

    private static final String SLING_DEFAULT_RESOURCE_TYPE = "sling/servlet/default";
    private static final Type DEFAULT_TYPE;
    private static final Map<String, Type> types = new HashMap<>();

    // TODO in many cases we're just interested in the presence of an annotation, but
    // not its value - refine this in the type system
    private static final String TRUE = "true";

    static {
        // Although these definitions are in Java code for this early prototype, the
        // plan is to move to a mini-language (DSL) to avoid having to use Java
        // code for what is actually just declarative statements.
        DEFAULT_TYPE = Builder
            .forResourceType(SLING_DEFAULT_RESOURCE_TYPE)
            .withAnnotation(VISIT_CONTENT, TRUE)
            .build();

        addType(
            Builder.forResourceType("cq:Page")
            .withAnnotation(DOCUMENT_ROOT, TRUE)
            .withAnnotation(NAVIGABLE, TRUE)
            .withAnnotation(VISIT_CONTENT, TRUE)
            .withAnnotation(VISIT_CONTENT_RESOURCE_NAME_PATTERN, "jcr:content")
            .withAnnotation(CONTENT_INCLUDE_PROPERTY_REGEXP, "sling:ResourceType|cq:tags")
            .withAnnotation(CONTENT_EXCLUDE_PROPERTY_REGEXP, "jcr:.*|cq:.*")
        );
        addType(
            Builder.forResourceType("sling:Folder")
            .withAnnotation(NAVIGABLE, TRUE)
        );
        addType(
            Builder.forResourceType("sling:OrderedFolder")
            .withAnnotation(NAVIGABLE, TRUE)
        );
        addType(
            Builder.forResourceType("sling:OrderedFolder")
            .withAnnotation(NAVIGABLE, TRUE)
        );
        addType(
            Builder.forResourceType("wknd/components/page")
            .withAnnotation(VISIT_CONTENT, TRUE)
        );
        addType(
            Builder.forResourceType("wknd/components/image")
            .withAnnotation(VISIT_CONTENT, TRUE)
            .withAnnotation(DEREFERENCE_BY_PATH, "fileReference")
        );
        addType(
            Builder.forResourceType("wknd/components/carousel")
            .withAnnotation(VISIT_CONTENT, TRUE)
        );

        // for /content/articles examples
        addType(
            Builder.forResourceType("samples/section")
            .withAnnotation(NAVIGABLE, TRUE)
            .withAnnotation(NAVIGATION_PROPERTIES_LIST, "name")
        );
        addType(
            Builder.forResourceType("samples/article")
            .withAnnotation(NAVIGABLE, TRUE)
        );
    }

    static void addType(Builder b) {
        final Type t = b.build();
        types.put(t.getResourceType(), t);
    }
    
    @Override
    public @Nullable Type getType(@NotNull Resource resource) {
        Type result = types.get(resource.getResourceType());
        if(result == null) {
            result = types.get(resource.getResourceSuperType());
        }
        if(result == null) {
            result = DEFAULT_TYPE;
        }
        return result;
    }

}