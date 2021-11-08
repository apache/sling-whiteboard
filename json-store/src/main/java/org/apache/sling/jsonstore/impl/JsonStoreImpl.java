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

import static org.apache.sling.jsonstore.api.JsonStoreConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jsonstore.api.JsonStore;
import org.osgi.service.component.annotations.Component;

@Component(service=JsonStore.class)
public class JsonStoreImpl implements JsonStore {
    
    public Resource createSite(Resource parent, String name) throws PersistenceException {
        final Resource site = createChild(parent, name, SITE_ROOT_RESOURCE_TYPE);
        createChild(site, SCHEMA_ROOT_NAME, SCHEMA_ROOT_RESOURCE_TYPE);
        createChild(site, ELEMENTS_ROOT_NAME, ELEMENTS_ROOT_RESOURCE_TYPE);
        createChild(site, CONTENT_ROOT_NAME, CONTENT_ROOT_RESOURCE_TYPE);
        site.getResourceResolver().commit();
        return site;
    }

    private Resource createChild(Resource parent, String name, String resourceType) throws PersistenceException {
        final Map<String, Object> props = new HashMap<>();
        props.put("sling:resourceType", resourceType);
        return parent.getResourceResolver().create(parent, name, props);
    }
}
