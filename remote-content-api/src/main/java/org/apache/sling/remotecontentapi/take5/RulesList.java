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

package org.apache.sling.remotecontentapi.take5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

class RulesList {
    private final List<ResourceRules> rules = new ArrayList<>();

    static class DefaultProcessor implements ResourceProcessor {
        private final String name;

        DefaultProcessor(String name) {
            this.name = name;
        }

        @Override
        public void process(Resource r, JsonObjectBuilder b, UrlBuilder urlb) throws IOException {
            b.add(name, r.getPath());
        }
    }

    static boolean matchAny(String in, String ... variants) {
        if(in == null) {
            return false;
        }
        for(String variant : variants) {
            if(in.equals(variant)) {
                return true;
            }
        }
        return false;
    }

    static class DefaultNavProcessor implements ResourceProcessor {
        @Override
        public void process(Resource r, JsonObjectBuilder b, UrlBuilder urlb) throws IOException {
            b.add("_id", r.getPath());
            b.add("_url", urlb.pathToUrl(r.getPath()));
            if(!r.getResourceType().isEmpty()) {
                b.add("_resourceType", r.getResourceType());
            }
        }
    }

    static class WkndImageContentProcessor implements ResourceProcessor {
        @Override
        public void process(Resource r, JsonObjectBuilder b, UrlBuilder urlb) throws IOException {
            final ValueMap vm = r.adaptTo(ValueMap.class);
            if(vm != null) {
                vm.entrySet().stream()
                .filter(entry -> "fileReference".equals(entry.getKey()))
                .forEach(entry -> b.add(entry.getKey(), String.valueOf(entry.getValue())));
            }
        }
    }

    static class DefaultContentProcessor implements ResourceProcessor {
        private final RulesList rules;

        DefaultContentProcessor(RulesList rules) {
            this.rules = rules;
        }

        @Override
        public void process(Resource r, JsonObjectBuilder b, UrlBuilder urlb) throws IOException {
            final ValueMap vm = r.adaptTo(ValueMap.class);
            if(vm != null) {
                vm.entrySet().stream().forEach(entry -> b.add(entry.getKey(), String.valueOf(entry.getValue())));
            }
            for(Resource child : r.getChildren()) {
                final JsonObjectBuilder childJson = Json.createObjectBuilder();
                if(rules.applyRules(child, true, childJson, urlb)) {
                    b.add(child.getName(), childJson);
                }
            }
        }
    }

    static boolean isNodeType(Resource r, String ... nodeTypes) {
        final ValueMap vm = r.adaptTo(ValueMap.class);
        if(vm != null) {
            return matchAny(vm.get("jcr:primaryType", String.class), nodeTypes);
        }
        return false;
    }

    RulesList() {
        rules.add(
            ResourceRules.builder(r -> isNodeType(r, "sling:Folder", "sling:OrderedFolder", "nt:folder"))
            .withNavigationProcessor(new DefaultNavProcessor())
            .withContentProcessor(new DefaultProcessor("FolderContent"))
            .build()
        );        
        rules.add(
            ResourceRules.builder(r -> "wknd/components/image".equals(r.getResourceType()))
            .withNavigationProcessor(null)
            .withContentProcessor(new WkndImageContentProcessor())
            .build()
        );
        rules.add(
            ResourceRules.builder(r -> isNodeType(r, "cq:Page"))
            .withNavigationProcessor(new DefaultNavProcessor())
            .withContentProcessor(new DefaultContentProcessor(this))
            .build()
        );
        rules.add(
            ResourceRules.builder(r -> "samples/article".equals(r.getResourceSuperType()))
            .withNavigationProcessor(new DefaultNavProcessor())
            .withContentProcessor(new DefaultContentProcessor(this))
            .build()
        );
        rules.add(
            ResourceRules.builder(r -> true)
            .withContentProcessor(new DefaultContentProcessor(this))
            .build()
        );
    }

    boolean applyRules(Resource resource, boolean contentMode, JsonObjectBuilder json, UrlBuilder urlb) throws IOException {
        final Optional<ResourceRules> activeRule = matchingRules(resource)
            .filter(rule -> contentMode ? rule.contentProcessor != null : rule.navigationProcessor != null)
            .findFirst();
        if(activeRule.isPresent()) {
            final ResourceProcessor p = contentMode ? activeRule.get().contentProcessor : activeRule.get().navigationProcessor;
            p.process(resource, json, urlb);
            return true;
        }
        return false;
    }

    private Stream<ResourceRules> matchingRules(Resource r) {
        return rules.stream().filter(rule -> rule.matcher.test(r));
    }
}