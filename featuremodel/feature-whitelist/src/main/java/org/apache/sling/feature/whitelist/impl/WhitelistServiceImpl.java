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

import org.apache.sling.feature.whitelist.WhitelistService;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class WhitelistServiceImpl implements WhitelistService {
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
}
