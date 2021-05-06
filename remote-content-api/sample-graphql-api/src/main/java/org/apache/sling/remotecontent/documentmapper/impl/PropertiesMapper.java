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

package org.apache.sling.remotecontent.documentmapper.impl;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.remotecontent.documentmapper.api.Annotations;
import org.apache.sling.remotecontent.documentmapper.api.MappingTarget;

class PropertiesMapper {
    void mapProperties(MappingTarget.TargetNode dest, Resource r, Annotations annot) {
        final ValueMap vm = r.adaptTo(ValueMap.class);
        if(vm != null) {
            for(Map.Entry<String, Object> e : vm.entrySet()) {
                if(!annot.includeProperty(e.getKey())) {
                    continue;
                }
                final Object value = e.getValue();
                if(value instanceof Object[]) {
                    dest.addValue(e.getKey(), Arrays.asList((Object[])value));
                } else if(value instanceof Calendar) {
                    dest.addValue(e.getKey(), ((Calendar)value).getTime().toString());
                } else {
                    dest.addValue(e.getKey(), String.valueOf(value));
                }
            }
        }
    }
}