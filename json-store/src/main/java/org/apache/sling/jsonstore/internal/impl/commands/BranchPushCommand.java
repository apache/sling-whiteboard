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

package org.apache.sling.jsonstore.internal.impl.commands;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jsonstore.internal.api.Command;
import org.apache.sling.jsonstore.internal.api.JsonStoreConstants;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

@Component(
    service=Command.class,
    property = {
        Command.SERVICE_PROP_NAMESPACE + "=branch",
        Command.SERVICE_PROP_NAME + "=push",
        Command.SERVICE_PROP_DESCRIPTION + "=" + BranchPushCommand.DESCRIPTION
    }
)
public class BranchPushCommand implements Command {

    public static final String DESCRIPTION = "pushes the current version of a document to a branch, using the input parameters shown here";
    public static final String F_BRANCH = "branch";
    public static final String F_PATH = "path";

    // TODO all path patterns should be configurable in a central place
    private final static Pattern SOURCE_PATTERN = Pattern.compile("/content/sites/([^/]+)/branches/authoring/(.*)");

    @Override
    public @NotNull JsonNode getInfo() {
        final ObjectNode example = JsonNodeFactory.instance.objectNode();
        example.put(F_BRANCH, "destination branch name");
        example.put(F_PATH, "full path of the document to push");

        final ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("command", getClass().getName());
        result.put("description", DESCRIPTION);
        result.replace("input", example);
        return result;
    }

    @Override
    public @NotNull JsonNode execute(ResourceResolver resolver, JsonNode input) throws IOException {
        final String branch = require(input, F_BRANCH);
        final String path = require(input, F_PATH);

        final Matcher m = SOURCE_PATTERN.matcher(path);
        if(!m.matches()) {
            throw new IOException(String.format("input path %s does not match %s", path, SOURCE_PATTERN));
        }
        final String destPath = String.format("/content/sites/%s/branches/%s/%s", m.group(1), branch, m.group(2));

        // TODO for now we copy the content, we might use JCR versioning instead
        final Resource src = resolver.getResource(null, path);
        if(src == null) {
            throw new IOException(String.format("Input resource not found: %s", path));
        }
        final Resource dest = ResourceUtil.getOrCreateResource(
            resolver,
            destPath,
            src.getResourceType(),
            JsonStoreConstants.FOLDER_RESOURCE_TYPE,
            false
        );

        final ValueMap srcMap = src.adaptTo(ValueMap.class);
        if(srcMap == null) {
            throw new IOException(String.format("Source %s does not adapt to %s", src.getPath(), ValueMap.class.getName()));
        }
        final ModifiableValueMap destMap = dest.adaptTo(ModifiableValueMap.class);
        if(destMap == null) {
            throw new IOException(String.format("Destination %s does not adapt to %s", dest.getPath(), ModifiableValueMap.class.getName()));
        }
        for(Map.Entry<String, Object> e : srcMap.entrySet()) {
            if(!e.getKey().startsWith("jcr:")) {
                destMap.put(e.getKey(), e.getValue());
            }
        }
        resolver.commit();

        // Prepare result
        final ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("status", String.format("Pushed %s to %s", src.getPath(), dest.getPath()));
        result.put(F_BRANCH, branch);
        result.put(F_PATH, src.getPath());
        result.put("destination", dest.getPath());
        return result;
    }

    private String require(JsonNode json, String field) throws IOException {
        final JsonNode f = json.get(field);
        if(f == null) {
            throw new IOException("Missing input field: " + field);
        }
        final String result = f.asText();
        if(result == null || result.length() < 1) {
            throw new IOException("Empty input field: " + field);
        }
        return result;
    }

}