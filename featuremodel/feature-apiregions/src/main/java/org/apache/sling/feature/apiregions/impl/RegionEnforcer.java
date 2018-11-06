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
package org.apache.sling.feature.apiregions.impl;

import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class RegionEnforcer implements ResolverHookFactory {
    public static String GLOBAL_REGION = "global";

    static final String PROPERTIES_FILE_PREFIX = "apiregions.";
    static final String IDBSNVER_FILENAME = "idbsnver.properties";
    static final String BUNDLE_FEATURE_FILENAME = "bundles.properties";
    static final String FEATURE_REGION_FILENAME = "features.properties";
    static final String REGION_PACKAGE_FILENAME = "regions.properties";

    final Map<Map.Entry<String, Version>, List<String>> bsnVerMap;
    final Map<String, Set<String>> bundleFeatureMap;
    final Map<String, Set<String>> featureRegionMap;
    final Map<String, Set<String>> regionPackageMap;
    final Set<String> enabledRegions;

    RegionEnforcer(Dictionary<String, Object> regProps, String regionsProp) throws IOException {
        File idbsnverFile = getDataFile(IDBSNVER_FILENAME);
        bsnVerMap = populateBSNVerMap(idbsnverFile);
        if (idbsnverFile != null) {
            regProps.put(IDBSNVER_FILENAME, idbsnverFile.getAbsolutePath());
        }

        File bundlesFile = getDataFile(BUNDLE_FEATURE_FILENAME);
        bundleFeatureMap = populateBundleFeatureMap(bundlesFile);
        if (bundlesFile != null) {
            regProps.put(BUNDLE_FEATURE_FILENAME, bundlesFile.getAbsolutePath());
        }

        File featuresFile = getDataFile(FEATURE_REGION_FILENAME);
        featureRegionMap = populateFeatureRegionMap(featuresFile);
        if (featuresFile != null) {
            regProps.put(FEATURE_REGION_FILENAME, featuresFile.getAbsolutePath());
        }

        File regionsFile = getDataFile(REGION_PACKAGE_FILENAME);
        regionPackageMap = populateRegionPackageMap(regionsFile);
        if (regionsFile != null) {
            regProps.put(REGION_PACKAGE_FILENAME, regionsFile.getAbsolutePath());
        }

        enabledRegions = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(regionsProp.split(","))));
    }

    private Map<Map.Entry<String, Version>, List<String>> populateBSNVerMap(File idbsnverFile) throws IOException {
        if (idbsnverFile != null && idbsnverFile.exists()) {
            Map<Map.Entry<String, Version>, List<String>> m = new HashMap<>();

            Properties p = new Properties();
            try (InputStream is = new FileInputStream(idbsnverFile)) {
                p.load(is);
            }

            for (String n : p.stringPropertyNames()) {
                String[] bsnver = p.getProperty(n).split("~");
                Map.Entry<String, Version> key = new AbstractMap.SimpleEntry<String, Version>(bsnver[0], Version.valueOf(bsnver[1]));
                List<String> l = m.get(key);
                if (l == null) {
                    l = new ArrayList<>();
                    m.put(key, l);
                }
                l.add(n);
            }

            Map<Map.Entry<String, Version>, List<String>> m2 = new HashMap<>();

            for (Map.Entry<Map.Entry<String, Version>, List<String>> entry : m.entrySet()) {
                m2.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
            }

            return Collections.unmodifiableMap(m2);
        } else {
            return Collections.emptyMap();
        }
    }

    private Map<String, Set<String>> populateBundleFeatureMap(File bundlesFile) throws IOException {
        return loadMap(bundlesFile);
    }

    private Map<String, Set<String>> populateFeatureRegionMap(File featuresFile) throws IOException {
        return loadMap(featuresFile);
    }

    private Map<String, Set<String>> populateRegionPackageMap(File regionsFile) throws IOException {
        return loadMap(regionsFile);
    }

    private Map<String, Set<String>> loadMap(File propsFile) throws IOException {
        Map<String, Set<String>> m = new HashMap<>();

        if (propsFile != null && propsFile.exists()) {
            Properties p = new Properties();
            try (InputStream is = new FileInputStream(propsFile)) {
                p.load(is);
            }

            for (String n : p.stringPropertyNames()) {
                String[] features = p.getProperty(n).split(",");
                m.put(n, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(features))));
            }
        }

        return Collections.unmodifiableMap(m);
    }

    private File getDataFile(String name) throws IOException {
        String fn = System.getProperty(PROPERTIES_FILE_PREFIX + name);
        if (fn == null)
            return null;
        return new File(fn);
    }

    @Override
    public ResolverHook begin(Collection<BundleRevision> triggers) {
        if (enabledRegions.size() == 0)
            return null;
        return new ResolverHookImpl(bsnVerMap, bundleFeatureMap, featureRegionMap, regionPackageMap);
    }
}
