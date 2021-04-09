/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.ddr.core;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Utils {

//    public static boolean filterSource(Map<String, List<String>> allowedDDRFilter, Map<String, List<String>> prohibitedDDRFilter, Resource source) {
//        if(allowedDDRFilter.isEmpty() && prohibitedDDRFilter.isEmpty()) { return true; }
//        if(source == null) { return false; }
//        ValueMap properties = source.getValueMap();
//        if(!allowedDDRFilter.isEmpty()) {
//            for (Entry<String, List<String>> filter : allowedDDRFilter.entrySet()) {
//                String propertyValue = properties.get(filter.getKey(), String.class);
//                if (propertyValue != null) {
//                    if (!filter.getValue().contains(propertyValue)) {
//                        return false;
//                    }
//                }
//            }
//        }
//        for(Entry<String,List<String>> filter: prohibitedDDRFilter.entrySet()) {
//            String propertyValue = properties.get(filter.getKey(), String.class);
//            if(propertyValue != null) {
//                if(filter.getValue().contains(propertyValue)) {
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
}
