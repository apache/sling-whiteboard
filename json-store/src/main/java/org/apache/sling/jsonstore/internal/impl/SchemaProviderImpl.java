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

package org.apache.sling.jsonstore.internal.impl;

import static org.apache.sling.jsonstore.internal.api.JsonStoreConstants.JSON_PROP_NAME;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.SpecVersionDetector;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jsonstore.internal.api.SchemaProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = SchemaProvider.class)
public class SchemaProviderImpl implements SchemaProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper mapper = new ObjectMapper();
    
    // TODO all path patterns should be configurable in a central place
    private final static Pattern EXTRACT_SITE_PATTERN = Pattern.compile("/content/sites/([^/]+)/(.*)");

    @Override
    public @Nullable JsonSchema getSchema(@NotNull ResourceResolver resolver, @NotNull String resourcePath, @NotNull String schemaRef) throws IOException {
        final String schemaResourcePath = getSchemaPath(resourcePath, schemaRef);
        final Resource schemaResource = resolver.getResource(schemaResourcePath);
        if(schemaResource == null) {
            return null;
        }

        final ValueMap vm = schemaResource.adaptTo(ValueMap.class);
        final String schemaStr = vm.get(JSON_PROP_NAME, String.class);
        if(schemaStr == null) {
            log.warn("Missing {} property on {}", JSON_PROP_NAME, schemaResource.getPath());
            return null;
        }

        return buildSchema(mapper.readTree(schemaStr));
    }

    @Override
    public @NotNull JsonSchema buildSchema(@NotNull JsonNode json) throws IOException {
        try {
            final SpecVersion.VersionFlag v = SpecVersionDetector.detect(json);
            return JsonSchemaFactory.getInstance(v).getSchema(json);
        } catch(JsonSchemaException jse) {
            throw new IOException("buildSchema failed", jse);
        }
    }

    @Override
    public @NotNull String getSchemaPath(String resourcePath, String schemaRef) throws IOException {
        if(resourcePath == null || resourcePath.length() == 0) {
            throw new IOException("Missing resourcePath");
        }
        if(schemaRef == null || schemaRef.length() == 0) {
            throw new IOException("Missing schemaRef");
        }
        final Matcher m = EXTRACT_SITE_PATTERN.matcher(resourcePath);
        if(m.matches()) {
            return String.format("/content/sites/%s/schema/%s", m.group(1), schemaRef);
        } else {
            throw new IOException(String.format("Resource path %s does not match %s", resourcePath, EXTRACT_SITE_PATTERN));
        }
    }
}