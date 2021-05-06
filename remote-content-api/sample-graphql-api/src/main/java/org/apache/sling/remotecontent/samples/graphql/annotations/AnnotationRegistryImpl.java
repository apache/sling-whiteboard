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

package org.apache.sling.remotecontent.samples.graphql.annotations;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.remotecontent.documentmapper.api.AnnotationRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import static org.apache.sling.remotecontent.documentmapper.api.AnnotationNames.*;

/** Temporary hardcoded type system for this prototype, until we
 *  have the actual type system
 *  NOTE that this is DUPLICATED from the http sample API module, we'll
 *  need to refactor!
 */

@Component(service=AnnotationRegistry.class)
public class AnnotationRegistryImpl implements AnnotationRegistry {

    private static final String SLING_DEFAULT_RESOURCE_TYPE = "sling/servlet/default";
    private static final Map<String, String> annotations = new HashMap<>();

    // TODO in many cases we're just interested in the presence of an annotation, but
    // not its value - refine this in the type system
    private static final String TRUE = "true";

    @Activate
    public void activate() {
        // Although these definitions are in Java code for this early prototype, the
        // plan is to move to a mini-language (DSL) to avoid having to use Java
        // code for what is actually just declarative statements.
        add(
            Builder.forResourceType("cq:Page")
            .withAnnotation(DOCUMENT_ROOT, TRUE)
            .withAnnotation(NAVIGABLE, TRUE)
            .withAnnotation(VISIT_CONTENT, TRUE)
            .withAnnotation(VISIT_CONTENT_RESOURCE_NAME_PATTERN, "jcr:content")
            .withAnnotation(CONTENT_INCLUDE_PROPERTY_REGEXP, "sling:ResourceType|cq:tags")
            .withAnnotation(CONTENT_EXCLUDE_PROPERTY_REGEXP, "jcr:.*|cq:.*")
        );
        add(
            Builder.forResourceType("sling:Folder")
            .withAnnotation(NAVIGABLE, TRUE)
        );
        add(
            Builder.forResourceType("sling:OrderedFolder")
            .withAnnotation(NAVIGABLE, TRUE)
        );
        add(
            Builder.forResourceType("sling:OrderedFolder")
            .withAnnotation(NAVIGABLE, TRUE)
        );
        add(
            Builder.forResourceType("wknd/components/page")
            .withAnnotation(VISIT_CONTENT, TRUE)
        );
        add(
            Builder.forResourceType("wknd/components/image")
            .withAnnotation(VISIT_CONTENT, TRUE)
            .withAnnotation(DEREFERENCE_BY_PATH, "fileReference")
        );
        add(
            Builder.forResourceType("wknd/components/carousel")
            .withAnnotation(VISIT_CONTENT, TRUE)
        );

        // for /content/articles examples
        add(
            Builder.forResourceType("samples/section")
            .withAnnotation(NAVIGABLE, TRUE)
            .withAnnotation(NAVIGATION_PROPERTIES_LIST, "name")
        );
        add(
            Builder.forResourceType("samples/article")
            .withAnnotation(NAVIGABLE, TRUE)
        );
    }

    private void add(Builder b) {
        b.getAnnotations().forEach(a -> annotations.put(a.getKey(), a.getValue()));
    }

    @Override
    public String getAnnotation(String resourceType, String annotationName) {
        return annotations.get(AnnotationImpl.makeKey(resourceType, annotationName));
    }

    @Override
    public boolean hasAnnotation(String resourceType, String annotationName) {
        return annotations.containsKey(AnnotationImpl.makeKey(resourceType, annotationName));
    }

}