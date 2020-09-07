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

package org.apache.sling.remotecontentapi.rcaservlet;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

public class PipelineContext {
    private final SlingHttpServletRequest request;
    public final Resource resource;
    public final JsonObjectBuilder navigation;
    public final JsonObjectBuilder metadata;
    public final JsonObjectBuilder children;
    private JsonObject content;

    public PipelineContext(SlingHttpServletRequest request) {
        this.request = request;
        resource = request.getResource();

        navigation = Json.createObjectBuilder();
        metadata = Json.createObjectBuilder();
        children = Json.createObjectBuilder();

        navigation.add("self", pathToUrl(resource.getPath()));
        if(resource.getParent() != null) {
            navigation.add("parent", pathToUrl(resource.getParent().getPath()));
        }
        metadata.add("_id", resource.getPath());
    }

    private void maybeAdd(JsonObjectBuilder target, String key, JsonObjectBuilder src) {
        final JsonObject jo = src.build();
        if(!jo.isEmpty()) {
            target.add(key, jo);
        }
    }

    public void setContent(JsonObject content) {
        this.content = content;
    }

    JsonObject build() {
        final JsonObjectBuilder b = Json.createObjectBuilder();
        maybeAdd(b, "navigation", navigation);
        maybeAdd(b, "metadata", metadata);
        maybeAdd(b, "children", children);
        if(content != null && !content.isEmpty()) {
            b.add("content", content);
        }
        return b.build();
    }

    public String pathToUrlNoJsonExtension(String path) {
        return String.format(
            "%s://%s:%d%s",
            request.getScheme(),
            request.getServerName(),
            request.getServerPort(),
            path
        );
    }

    public String pathToUrl(String path) {
        return String.format(
            "%s.%s.%s",
            pathToUrlNoJsonExtension(path),
            request.getRequestPathInfo().getSelectorString(),
            request.getRequestPathInfo().getExtension()
        );
    }
}