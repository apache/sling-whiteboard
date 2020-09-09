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

package org.apache.sling.remotecontentapi.xyz;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.remotecontentapi.rcaservlet.P;
import org.apache.sling.servlethelpers.internalrequests.ServletInternalRequest;

class ProcessingRuleSelector {
    private final ServletResolver servletResolver;

    enum RuleType {
        METADATA, CHILDREN, CONTENT
    };

    static class DefaultMetadataRule implements ProcessingRule {
        private final Resource resource;

        DefaultMetadataRule(Resource r) {
            resource = r;
        }

        @Override
        public void process(JsonObjectBuilder b, UrlBuilder urlb) {
            // nothing to for do for now
        }
    }

    static class DefaultChildrenRule implements ProcessingRule {
        private final Resource resource;

        DefaultChildrenRule(Resource r) {
            resource = r;
        }

        @Override
        public void process(JsonObjectBuilder b, UrlBuilder urlb) {
            for (Resource child : resource.getChildren()) {
                if (P.ignoreResource(child.getName())) {
                    continue;
                }
                final JsonObjectBuilder childBuilder = Json.createObjectBuilder();
                final ValueMap vm = child.adaptTo(ValueMap.class);
                if (vm != null) {
                    childBuilder.add("_path", child.getPath());
                    childBuilder.add("_url", urlb.pathToUrl(child.getPath()));
                    P.maybeAdd(childBuilder, "sling:resourceType", "_resourceType", vm);
                    P.maybeAddOneOf(childBuilder, "title", vm, P.TITLE_PROPS);
                    P.maybeAddOneOf(childBuilder, "name", vm, P.NAME_PROPS);
                }
                b.add(child.getName(), childBuilder.build());
            }
        }
    }

    class DefaultContentRule implements ProcessingRule {
        private final Resource resource;

        DefaultContentRule(Resource r) {
            resource = r;
        }

        @Override
        public void process(JsonObjectBuilder b, UrlBuilder urlb) throws IOException {
            final String jsonResponse = new ServletInternalRequest(servletResolver, resource)
            .withSelectors("s:cagg")
            .execute()
            .getResponseAsString();

        if(!jsonResponse.trim().isEmpty()) {
            try (JsonReader parser = Json.createReader(new StringReader(jsonResponse))) {
                b.add("xyz", parser.readObject());
            }
        }

        }
    }

    ProcessingRuleSelector(ServletResolver sr) {
        servletResolver = sr;
    }

    ProcessingRule getRule(RuleType t, Resource r) {
        switch(t) {
            case METADATA : return new DefaultMetadataRule(r);
            case CHILDREN : return new DefaultChildrenRule(r);
            case CONTENT : return new DefaultContentRule(r);
        }
        return null;
    }
}