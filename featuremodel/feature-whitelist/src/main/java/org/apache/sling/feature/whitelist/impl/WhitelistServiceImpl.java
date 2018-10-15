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
package org.apache.sling.feature.whitelist.impl;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.service.Features;
import org.apache.sling.feature.whitelist.WhitelistService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

@Component(immediate=true)
class WhitelistServiceImpl implements WhitelistService {

    @Reference
    Features featuresService;

    final Map<String, Set<String>> featureRegions = new ConcurrentHashMap<>();
    final Map<String, Set<String>> regionPackages = new ConcurrentHashMap<>();


    @Activate
    public void activate() {
        Map<String, Set<String>> frMap = new HashMap<>();
        Map<String, Set<String>> rpMap = new HashMap<>();

        for (Extension ex : featuresService.getCurrentFeature().getExtensions()) {
            if (!"api-region".equals(ex.getName()))
                continue;

            JsonReader reader = Json.createReader(new StringReader(ex.getJSON()));
            JsonArray ja = reader.readArray();
            for (JsonValue jv : ja) {
                if (jv instanceof JsonObject) {
                    JsonObject jo = (JsonObject) jv;
                    String name = jo.getString("name");
                    String feature = jo.getString("org-feature");

                    Set<String> regions = frMap.get(feature);
                    if (regions == null) {
                        regions = new HashSet<>();
                        frMap.put(feature, regions);
                    }
                    regions.add(name);

                    Set<String> packages = rpMap.get(name);
                    if (packages == null) {
                        packages = new HashSet<>();
                        rpMap.put(name, packages);
                    }

                    JsonArray xja = jo.getJsonArray("exports");
                    for (JsonValue ev : xja) {
                        if (ev instanceof JsonString) {
                            JsonString js = (JsonString) ev;
                            packages.add(js.getString());
                        }
                    }
                }
            }
        }

        // Store in fields as immutable sets
        featureRegions.clear();
        for (Map.Entry<String, Set<String>> entry : frMap.entrySet()) {
            featureRegions.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }

        regionPackages.clear();
        for (Map.Entry<String, Set<String>> entry : rpMap.entrySet()) {
            regionPackages.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }
    }


    @Override
    public Set<String> listRegions(String feature) {
        Set<String> regions = featureRegions.get(feature);
        if (regions == null)
            return Collections.emptySet();
        else
            return regions;
    }


    @Override
    public Set<String> listPackages(String region) {
        Map<String, Set<String>> packages = regionPackages;
        if (packages == null)
            return Collections.emptySet();
        else
            return packages.get(region);
    }



//    @Override
//    public Boolean regionWhitelistsPackage(String region, String packageName) {
//        // TODO Auto-generated method stub
//        return null;
//    }
    /*
    final Map<String, Set<String>> featureRegionMapping;
    final Map<String, Set<String>> regionPackageMapping;

    WhitelistServiceImpl(Map<String, Set<String>> regionPackages,
            Map<String, Set<String>> featureRegions) {
        Set<String> allRegions = new HashSet<>();

        Map<String, Set<String>> frm = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : featureRegions.entrySet()) {
            Set<String> regions = Collections.unmodifiableSet(new HashSet<>(entry.getValue()));
            allRegions.addAll(regions);
            frm.put(entry.getKey(), regions);
        }
        featureRegionMapping = Collections.unmodifiableMap(frm);

        Map<String, Set<String>> rpm = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : regionPackages.entrySet()) {
            String region = entry.getKey();
            rpm.put(region,
                    Collections.unmodifiableSet(new HashSet<>(entry.getValue())));
            allRegions.remove(region);
        }

        // If there are more regions mentioned but these don't have any package
        // mappings, give them an empty mapping
        for (String region : allRegions) {
            rpm.put(region, Collections.emptySet());
        }
        regionPackageMapping = Collections.unmodifiableMap(rpm);
    }

    @Override
    public Set<String> listRegions(String featureID) {
        if (featureID == null)
            return null;

        return featureRegionMapping.get(featureID);
    }

    @Override
    public Boolean regionWhitelistsPackage(String region, String packageName) {
        Set<String> packages = regionPackageMapping.get(region);

        if (packages == null)
            return null;

        return packages.contains(packageName);
    }
    */
}
