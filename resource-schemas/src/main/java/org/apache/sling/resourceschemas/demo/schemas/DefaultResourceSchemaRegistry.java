/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.resourceschemas.demo.schemas;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import org.apache.sling.resourceschemas.api.*;
import java.util.Map;
import org.osgi.service.component.annotations.Component;

/** Temporary ResourceSchemaRegistry that provides a set of
  demo ResourceSchemas */
@Component(service = ResourceSchemaRegistry.class,
    property = {
            "service.description=Sling Resource Schemas Default ResourceSchema Registry",
            "service.vendor=The Apache Software Foundation"
    })
public class DefaultResourceSchemaRegistry implements ResourceSchemaRegistry {

    private final Map<String, ResourceSchema> schemas = new HashMap<>();
    
    public DefaultResourceSchemaRegistry() {
        // TODO schemas are hardcoded for now
        for(ResourceSchema m : DemoSchemas.getSchemas()) {
            schemas.put(m.getName(), m);
        }
    }
    
    @Override
    public ResourceSchema getSchema(String resourceType) {
        return schemas.get(resourceType);
    }

    @Override
    public Collection<ResourceSchema> getSchemas() {
        return Collections.unmodifiableCollection(schemas.values());
    }
}