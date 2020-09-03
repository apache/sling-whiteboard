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
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.apache.sling.api.resource.ValueMap;

class P {
    static final String [] IGNORE_RESOURCE_PREIX = { "jcr:", "rep:", "oak:" };
    static final String [] TITLE_PROPS = { "jcr:title", "title" };
    static final String [] NAME_PROPS = { "jcr:name", "name" };
    static final String [] TEXT_PROPS = { "jcr:text", "text" };
    static final String [] DESCRIPTION_PROPS = { "jcr:description", "description" };

    static boolean maybeAdd(JsonObjectBuilder b, String propName, String jsonName, ValueMap vm) {
        if(vm.containsKey(propName)) {
            final Object value = vm.get(propName);
            if(value != null) {
                if(value instanceof Object[]) {
                    final JsonArrayBuilder a = Json.createArrayBuilder();
                    for(Object o : (Object[])value) {
                        a.add(o.toString());
                    }
                    b.add(jsonName, a.build());
                } else {
                    b.add(jsonName, value.toString());
                }
            }
            return true;
        }
        return false;
    }

    static void maybeAddOneOf(JsonObjectBuilder b, String propName, ValueMap vm, String [] props) {
        for(String prop : props) {
            if(maybeAdd(b, prop, propName, vm)) {
                break;
            }
        }
    }

    static boolean ignoreProperty(String key) {
        return key.startsWith("jcr:");
    }

    static boolean ignoreResource(String name) {
        for(String prefix : IGNORE_RESOURCE_PREIX) {
            if(name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    static String convertName(String in) {
        return in.replace("sling:", "_");
    }

    static boolean isMetadata(String propName) {
        return propName.startsWith("sling:");
    }
}