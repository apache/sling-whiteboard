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
import static org.apache.sling.jsonstore.api.JsonStoreConstants.JSON_SCHEMA_FIELD;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jsonstore.api.DataTypeValidator;
import org.apache.sling.jsonstore.api.SchemaProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataTypeValidator.class)
public class ContentValidator implements DataTypeValidator {

    @Reference
    private SchemaProvider schemaProvider;

    @Override
    public boolean validate(ResourceResolver resolver, JsonNode json, String site, String dataType) throws DataTypeValidator.ValidatorException {
        if(!CONTENT_DATA_TYPE.equals(dataType) && !ELEMENTS_DATA_TYPE.equals(dataType)) {
            return false;
        }

        // TODO fixed schema for now -> extract from incoming document, $schema ?
        final JsonNode schemaField = json.get(JSON_SCHEMA_FIELD);
        if(schemaField == null) {
            throw new ValidatorException("Schema field missing in incoming document:" + JSON_SCHEMA_FIELD);
        }
        final String schemaRef = schemaField.asText();
        JsonSchema schema = null;
        try {
            schema = schemaProvider.getSchema(resolver, site, schemaRef);
            if(schema == null) {
                throw new ValidatorException("Schema not found: " + schemaRef);
            }
        } catch(IOException ioe) {
            throw new ValidatorException("Error retrieving schema " + schemaRef, ioe);
        }
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