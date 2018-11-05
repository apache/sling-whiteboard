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

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

class ResolverHookImpl implements ResolverHook {
    private static final Logger LOG = Logger.getLogger(ResolverHookImpl.class.getName());

    final Map<Map.Entry<String, Version>, List<String>> bsnVerMap;
    final Map<String, Set<String>> bundleFeatureMap;
    final Map<String, Set<String>> featureRegionMap;
    final Map<String, Set<String>> regionPackageMap;

    public ResolverHookImpl(Map<Entry<String, Version>, List<String>> bsnVerMap, Map<String, Set<String>> bundleFeatureMap,
            Map<String, Set<String>> featureRegionMap, Map<String, Set<String>> regionPackageMap) {
        this.bsnVerMap = bsnVerMap;
        this.bundleFeatureMap = bundleFeatureMap;
        this.featureRegionMap = featureRegionMap;
        this.regionPackageMap = regionPackageMap;
    }

    @Override
    public void filterResolvable(Collection<BundleRevision> candidates) {
        // Nothing to do
    }

    @Override
    public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
        // Nothing to do
    }

    @Override
    public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
        // Filtering is only on package resolution. Any other kind of resolution is not limited
        if (!PackageNamespace.PACKAGE_NAMESPACE.equals(requirement.getNamespace()))
            return;

        Bundle reqBundle = requirement.getRevision().getBundle();
        long reqBundleID = reqBundle.getBundleId();
        String reqBundleName = reqBundle.getSymbolicName();
        Version reqBundleVersion = reqBundle.getVersion();

        Set<String> reqRegions = new HashSet<>();
        List<String> reqFeatures = new ArrayList<>();
        List<String> aids = bsnVerMap.get(new AbstractMap.SimpleEntry<String, Version>(reqBundleName, reqBundleVersion));
        if (aids != null) {
            for (String aid : aids) {
                Set<String> fid = bundleFeatureMap.get(aid);
                if (fid != null)
                    reqFeatures.addAll(fid);
            }

            for (String feature : reqFeatures) {
                Set<String> fr = featureRegionMap.get(feature);
                if (fr != null) {
                    reqRegions.addAll(fr);
                }
            }
        } else {
            // Bundle is not coming from a feature
        }

        Set<BundleCapability> coveredCaps = new HashSet<>();

        nextCapability:
        for (BundleCapability bc : candidates) {
            BundleRevision rev = bc.getRevision();

            Bundle capBundle = rev.getBundle();
            long capBundleID = capBundle.getBundleId();
            if (capBundleID == 0) {
                // always allow capability from the system bundle
                coveredCaps.add(bc);
                continue nextCapability;
            }

            if (capBundleID == reqBundleID) {
                // always allow capability from same bundle
                coveredCaps.add(bc);
                continue nextCapability;
            }

            String capBundleName = capBundle.getSymbolicName();
            Version capBundleVersion = capBundle.getVersion();

            List<String> capBundleArtifacts = bsnVerMap.get(new AbstractMap.SimpleEntry<String, Version>(capBundleName, capBundleVersion));
            if (capBundleArtifacts == null) {
                // Capability is not in any feature, everyone can access
                coveredCaps.add(bc);
                continue nextCapability;
            }

            List<String> capFeatures = new ArrayList<>();
            for (String ba : capBundleArtifacts) {
                Set<String> capfeats = bundleFeatureMap.get(ba);
                if (capfeats != null)
                    capFeatures.addAll(capfeats);
            }

            if (capFeatures.isEmpty())
                capFeatures = Collections.singletonList(null);

            for (String capFeat : capFeatures) {
                if (capFeat == null) {
                    // always allow capability not coming from a feature
                    coveredCaps.add(bc);
                    continue nextCapability;
                }

                if (reqFeatures.contains(capFeat)) {
                    // Within a single feature everything can wire to everything else
                    coveredCaps.add(bc);
                    continue nextCapability;
                }

                Set<String> capRegions = featureRegionMap.get(capFeat);
                if (capRegions == null) {
                    // If the feature hosting the capability has no regions defined, everyone can access
                    coveredCaps.add(bc);
                    continue nextCapability;
                }

                HashSet<String> sharedRegions = new HashSet<String>(reqRegions);
                sharedRegions.retainAll(capRegions);

                Object pkg = bc.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                if (pkg instanceof String) {
                    String packageName = (String) pkg;

                    Set<String> globalPackages = regionPackageMap.get("global");
                    if (globalPackages != null && globalPackages.contains(packageName)) {
                        // If the export is in the global region everyone can access
                        coveredCaps.add(bc);
                        continue nextCapability;
                    }

                    for (String region : sharedRegions) {
                        Set<String> regionPackages = regionPackageMap.get(region);
                        if (regionPackages != null && regionPackages.contains(packageName)) {
                            // If the export is in a region that the feature is also in, then allow
                            coveredCaps.add(bc);
                            continue nextCapability;
                        }
                    }
                }
            }
        }

        // Remove any capabilities that are not covered
        if (candidates.retainAll(coveredCaps)) {
            LOG.log(Level.INFO,
                    "Removed one ore more candidates for requirement {0} as they are not in the correct region", requirement);
        }
    }

    @Override
    public void end() {
        // Nothing to do
    }
}
