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

import static org.apache.sling.jsonstore.api.JsonStoreConstants.CONTENT_DATA_TYPE;
import static org.apache.sling.jsonstore.api.JsonStoreConstants.ELEMENTS_DATA_TYPE;
import static org.apache.sling.jsonstore.api.JsonStoreConstants.SCHEMA_DATA_TYPE;
import static org.apache.sling.jsonstore.api.JsonStoreConstants.STORE_ROOT_PATH;

import java.util.Set;

import static org.apache.sling.jsonstore.api.JsonStoreConstants.JSON_PROP_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jsonstore.api.JsonStoreValidator;
import org.osgi.service.component.annotations.Component;

@Component(service = JsonStoreValidator.class)
public class ContentValidator implements JsonStoreValidator {

    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);

    @Override
    public boolean validate(ResourceResolver resolver, JsonNode json, String site, String dataType) throws JsonStoreValidator.ValidatorException {
        if(!CONTENT_DATA_TYPE.equals(dataType) && !ELEMENTS_DATA_TYPE.equals(dataType)) {
            return false;
        }

        // TODO fixed schema for now
        final String schemaRef = "test/example";
        final String schemaPath = String.format("%s/%s/%s/%s", STORE_ROOT_PATH, site, SCHEMA_DATA_TYPE, schemaRef);
        final Resource schemaResource = resolver.getResource(schemaPath);
        if(schemaResource == null) {
            throw new ValidatorException("Schema Resource not found: " + schemaPath); 
        }

        final ValueMap vm = schemaResource.adaptTo(ValueMap.class);
        final String schemaStr = vm.get(JSON_PROP_NAME, String.class);
        if(schemaStr == null) {
            throw new ValidatorException("Missing " + JSON_PROP_NAME + "  property on schema resource " + schemaResource.getPath());
        }

        final JsonSchema schema = factory.getSchema(schemaStr);
        final Set<ValidationMessage> msgs = schema.validate(json);
        if(!msgs.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for(ValidationMessage msg : msgs) {
                sb.append(msg.toString());
                sb.append("\n");
            }
            throw new ValidatorException(sb.toString());
        }
        return true;
    }
}