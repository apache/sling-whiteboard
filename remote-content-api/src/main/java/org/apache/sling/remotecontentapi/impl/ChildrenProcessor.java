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

package org.apache.sling.remotecontentapi.impl;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

class ChildrenProcessor implements JsonProcessor {
    @Override
    public void process(PipelineContext pc) {
        if(pc.resource.hasChildren()) {
            for(Resource child: pc.resource.getChildren()) {
                if(P.ignoreResource(child.getName())) {
                    continue;
                }
                final JsonObjectBuilder childBuilder = Json.createObjectBuilder();
                final ValueMap vm = child.adaptTo(ValueMap.class);
                if(vm != null) {
                    childBuilder.add("_path", child.getPath());
                    childBuilder.add("_url", pc.pathToUrl(child.getPath()));
                    P.maybeAdd(childBuilder, "sling:resourceType", "_resourceType", vm);
                    P.maybeAddOneOf(childBuilder, "title", vm, P.TITLE_PROPS);
                    P.maybeAddOneOf(childBuilder, "name", vm, P.NAME_PROPS);
                }
                pc.children.add(child.getName(), childBuilder.build());
            }
        }
    }
}