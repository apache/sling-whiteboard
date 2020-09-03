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

package org.apache.sling.remotecontentapi.hardcodedfirstshot;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

class ContentProcessor implements JsonProcessor {
    private static final int MAX_RECURSION = 99;
    private static final String JCR_CONTENT = "jcr:content";

    @Override
    public void process(PipelineContext pc) {
        processResource(pc, pc.content, pc.resource, true, MAX_RECURSION);
    }

    private boolean isNodeType(ValueMap vm, String nodeType) {
        return vm == null ? false : nodeType.equals(vm.get("jcr:primaryType", String.class));
    }

    private boolean ignoreResource(Resource r) {
        final String name = r.getName();
        if(name.startsWith("cq:")) {
            return !name.equals("cq:tags");
        }
        final ValueMap vm = r.adaptTo(ValueMap.class);
        if(isNodeType(vm, "cq:Page")) {
                return true;
        }
        return false;
    }

    private String convertPropertyName(String name) {
        if(!name.contains(":")) {
            return name;
        } else if(name.equals("jcr:title")) {
            return "title";
        } else if(name.equals("jcr:description")) {
            return "description";
        } else if(name.equals("sling:resourceType")) {
            return "_resourceType";
        } else {
            return null;
        }
    }

    private void processResource(PipelineContext pc, JsonObjectBuilder json, Resource r, boolean contentRootMode, int maxRecursion) {
        if(maxRecursion <= 0) {
            return;
        }
        final ValueMap vm = r.adaptTo(ValueMap.class);

        if(isNodeType(vm, "nt:file")) {
            json.add("url", pc.pathToUrlNoJsonExtension(r.getPath()));
            return;
        }
        
        if(vm != null) {
            for(String key : vm.keySet()) {
                final String newName = convertPropertyName(key);
                if(newName != null) {
                    P.maybeAdd(json, key, newName, vm);
                }
            }
        }

        if(contentRootMode) {
            final Resource content = r.getChild(JCR_CONTENT);
            if(content != null) {
                json.addAll(visitContentResource(pc, content, maxRecursion - 1));
            }
        } else if(r.hasChildren()) {
            final JsonObjectBuilder b = Json.createObjectBuilder();
            for(Resource child : r.getChildren()) {
                if(!ignoreResource(child)) {
                    b.add(child.getName(), visitContentResource(pc, child, maxRecursion - 1));
                }
            }
            json.addAll(b);
        }
    }

    private JsonObjectBuilder visitContentResource(PipelineContext pc, Resource r, int maxRecursion) {
        final JsonObjectBuilder b = Json.createObjectBuilder();
        processResource(pc, b, r, false, maxRecursion - 1);
        return b;
    }
}