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

package org.apache.sling.jsonstore.impl;

import com.networknt.schema.SpecVersion;
import static org.apache.sling.jsonstore.api.JsonStoreConstants.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jsonstore.api.JsonStore;
import org.osgi.service.component.annotations.Component;

@Component(service=JsonStore.class)
public class JsonStoreImpl implements JsonStore {

    private final ObjectMapper mapper = new ObjectMapper();

    public Resource createSite(Resource parent, String name) throws PersistenceException {
        final ResourceResolver rr = parent.getResourceResolver();
        final Resource site = rr.create(parent, name, getProps(SITE_ROOT_RESOURCE_TYPE));
        rr.create(site, SCHEMA_ROOT_NAME, getProps(SCHEMA_ROOT_RESOURCE_TYPE));
        rr.create(site, ELEMENTS_ROOT_NAME, getProps(ELEMENTS_ROOT_RESOURCE_TYPE));
        rr.create(site, CONTENT_ROOT_NAME, getProps(CONTENT_ROOT_RESOURCE_TYPE));
        rr.commit();
        return site;
    }

    private Map<String, Object> getProps(String resourceType) {
        final Map<String, Object> props = new HashMap<>();
        props.put("sling:resourceType", resourceType);
        return props;
    }

    @Override
    public Resource createOrUpdateSchema(Resource parent, String resourceType, JsonNode schema) throws PersistenceException,IOException {

        // Validate the schema
        final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        factory.getSchema(schema);

        final ResourceResolver rr = parent.getResourceResolver();
        final String path = buildSchemaPath(parent, resourceType);

        // TODO stream this instead?
        final String json = mapper.writeValueAsString(schema);
        Resource r = rr.getResource(parent, path);
        if(r == null) {
            final Map<String, Object> props = getProps(SCHEMA_RESOURCE_TYPE);
            props.put(JSON_BLOB_PROPERTY, json);
            r = rr.create(parent, resourceType, props);
        } else {
            final ModifiableValueMap vm = r.adaptTo(ModifiableValueMap.class);
            vm.put(JSON_BLOB_PROPERTY, json);
        }
        rr.commit();
        return r;
    }

    static String buildSchemaPath(Resource parent, String resourceType) {
        return String.format("%s/%s", parent.getPath(), resourceType);
    }
}
