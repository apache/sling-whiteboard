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

import static org.apache.sling.jsonstore.internal.api.JsonStoreConstants.SCHEMA_DATA_TYPE;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jsonstore.internal.api.DataTypeValidator;
import org.apache.sling.jsonstore.internal.api.SchemaProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataTypeValidator.class)
public class SchemaValidator implements DataTypeValidator {

    @Reference
    private SchemaProvider schemaProvider;
    
    @Override
    public boolean validate(ResourceResolver resolver, JsonNode json, String site, String dataType) throws DataTypeValidator.ValidatorException {
        if(!SCHEMA_DATA_TYPE.equals(dataType)) {
            return false;
        }
        schemaProvider.buildSchema(json);
        return true;
    }
}