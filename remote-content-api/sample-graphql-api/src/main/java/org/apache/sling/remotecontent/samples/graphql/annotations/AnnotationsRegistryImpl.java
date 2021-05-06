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

import org.apache.sling.remotecontent.documentmapper.api.Annotations;
import org.apache.sling.remotecontent.documentmapper.api.AnnotationsRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/** Temporary hardcoded type system for this prototype, until we
 *  have the actual type system
 *  NOTE that this is DUPLICATED from the http sample API module, we'll
 *  need to refactor!
 */

@Component(service=AnnotationsRegistry.class)
public class AnnotationsRegistryImpl implements AnnotationsRegistry {

    private static final Map<String, Annotations> annotations = new HashMap<>();
    private static final Annotations DEFAULT_ANNOTATIONS;

    static {
        DEFAULT_ANNOTATIONS = Annotations.forResourceType("sling/servlet/default")
            .withVisitContent(true)
            .build();
    }

    // TODO in many cases we're just interested in the presence of an annotation, but
    // not its value - refine this in the type system

    @Activate
    public void activate() {
        add(
            Annotations.forResourceType("cq:Page")
            .withDocumentRoot(true)
            .withNavigable(true)
            .withVisitContent(true)
            .withVisitContentChildResourceNamePattern("jcr:content")
            .withIncludePropertyPattern("sling:ResourceType|cq:tags")
            .withExcludePropertyPattern("jcr:.*|cq:.*")
        );
        add(
            Annotations.forResourceType("wknd/components/page")
            // TODO shall we only have "visit content"?
            .withDocumentRoot(true)
            .withVisitContent(true)
            .withIncludePropertyPattern("sling:ResourceType|jcr:description")
            .withExcludePropertyPattern("jcr:.*|cq:.*")
        );
        add(
            Annotations.forResourceType("wknd/components/image")
            .withVisitContent(true)
            .withDereferenceByPathProperties("fileReference")
        );
        /*
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
        */
    }

    private void add(Annotations.Builder b) {
        final Annotations a = b.build();
        annotations.put(a.getResourceType(), a);
    }

    @Override
    public Annotations getAnnotations(String resourceType) {
        Annotations result = annotations.get(resourceType);
        return result == null ? DEFAULT_ANNOTATIONS : result;
    }
}