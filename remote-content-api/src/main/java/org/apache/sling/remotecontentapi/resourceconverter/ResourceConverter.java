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

package org.apache.sling.remotecontentapi.resourceconverter;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.remotecontentapi.hardcodedfirstshot.P;

public class ResourceConverter {
    public static interface Context {
        /** Return the full URL to access the given path */
        String getUrlForPath(String path, boolean includeApiSelectorsAndExtension);

        /** Return r's path relative to the Resource being rendered */
        String getRelativePath(Resource r);
    }

    private final Context context;
    private final Resource resource;

    static interface ResourceProcessor {
        void process(JsonObjectBuilder json, Resource r);
    }

    static interface PropertyProcessor {
        void process(JsonObjectBuilder json, String key, Object value);
    }

    static class DefaultResourceProcessor implements ResourceProcessor {
        private final Resource contentRoot;
        private final Context context;

        DefaultResourceProcessor(Context context, Resource contentRoot) {
            this.contentRoot = contentRoot;
            this.context = context;
        }

        public void process(JsonObjectBuilder json, Resource r) {
            final JsonObjectBuilder b = Json.createObjectBuilder();
            final ValueMap vm = r.adaptTo(ValueMap.class);
            if(vm != null) {
                final PropertyProcessor pp = new DefaultPropertyProcessor();
                for(Map.Entry<String, Object> entry : vm.entrySet()) {
                    pp.process(json, entry.getKey(), entry.getValue());
                }
            }

            // Special treatment for nt:file nodes
            if(isNodeType(vm, "nt:file")) {
                json.add("url", context.getUrlForPath(r.getPath(), false));
                return;
            }
    
            if(r.getPath().equals(contentRoot.getPath())) {
                // At the root, recurse only in the jcr:content child
                final Resource content = r.getChild("jcr:content");
                if(content != null) {
                    process(b, content);
                }
            } else if(r.hasChildren()) {
                for(Resource child : r.getChildren()) {
                    process(b, child);
                }
            }

            if(r.getName().equals("jcr:content")) {
                json.add("_components", b.build());
            } else if(vm!=null && vm.containsKey("sling:resourceType")) {
                final JsonObjectBuilder comp = Json.createObjectBuilder();
                comp.add("_name", r.getName());
                comp.addAll(b);
                json.add("_component", comp);
            } else {
                json.add(r.getName(), b.build());
            }
        }
    }

    private static boolean isNodeType(ValueMap vm, String nodeType) {
        return vm == null ? false : nodeType.equals(vm.get("jcr:primaryType", String.class));
    }

    static class DefaultPropertyProcessor implements PropertyProcessor {
        public void process(JsonObjectBuilder json, String key, Object value) {
            if(value != null) {
                final String newName = processName(key);
                if(newName != null) {
                    P.addValue(json, newName, value);
                }
            }
        }

        private String processName(String propertyName) {
            if(!propertyName.contains(":")) {
                return propertyName;
            } else if(propertyName.equals("jcr:title")) {
                return "title";
            } else if(propertyName.equals("jcr:description")) {
                return "description";
            } else if(propertyName.equals("sling:resourceType")) {
                return "_resourceType";
            } else {
                return null;
            }
        }
    }

    public ResourceConverter(Resource r, Context ctx) {
        context = ctx;
        resource = r;
    }

    public JsonObjectBuilder getJson() {
        final JsonObjectBuilder b = Json.createObjectBuilder();
        new DefaultResourceProcessor(context, resource).process(b, resource);
        return b;
    }
}