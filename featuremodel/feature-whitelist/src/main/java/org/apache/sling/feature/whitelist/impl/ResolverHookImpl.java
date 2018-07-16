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

import org.apache.sling.feature.service.Features;
import org.apache.sling.feature.whitelist.WhitelistService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

class ResolverHookImpl implements ResolverHook {
    private static final long SERVICE_WAIT_TIMEOUT = 60000;

    private final ServiceTracker<Features, Features> featureServiceTracker;
    private final WhitelistService whitelistService;

    public ResolverHookImpl(ServiceTracker<Features, Features> tracker,
            WhitelistService wls) {
        featureServiceTracker = tracker;
        whitelistService = wls;
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

        System.out.println("*** Filter Matches: " + requirement);
        Bundle reqBundle = requirement.getRevision().getBundle();
        long reqBundleID = reqBundle.getBundleId();
        String reqBundleName = reqBundle.getSymbolicName();
        Version reqBundleVersion = reqBundle.getVersion();

        try {
            Features fs = featureServiceTracker.waitForService(SERVICE_WAIT_TIMEOUT);

            // The Feature Service could not be found, skip candidate pruning
            if (fs == null) {
                WhitelistEnforcer.LOG.warning("Could not obtain the feature service, no whitelist enforcement");
                return;
            }

            Set<String> reqFeatures = fs.getFeaturesForBundle(reqBundleName, reqBundleVersion);
            Set<String> regions;
            if (reqFeatures == null) {
                regions = Collections.emptySet();
                reqFeatures = Collections.emptySet();
            } else {
                regions = new HashSet<>();
                for (String feature : reqFeatures) {
                    regions.addAll(whitelistService.listRegions(feature));
                }
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

                Set<String> capFeatures = fs.getFeaturesForBundle(capBundleName, capBundleVersion);
                if (capFeatures == null || capFeatures.isEmpty())
                    capFeatures = Collections.singleton(null);

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

                    if (whitelistService.listRegions(capFeat) == null) {
                        // If the feature hosting the capability has no regions defined, everyone can access
                        coveredCaps.add(bc);
                        continue nextCapability;
                    }

                    Object pkg = bc.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                    if (pkg instanceof String) {
                        String packageName = (String) pkg;
                        if (Boolean.TRUE.equals(whitelistService.regionWhitelistsPackage(WhitelistService.GLOBAL_REGION, packageName))) {
                            // If the export is in the global region everyone can access
                            coveredCaps.add(bc);
                            continue nextCapability;
                        }

                        for (String region : regions) {
                            if (!Boolean.FALSE.equals(whitelistService.regionWhitelistsPackage(region, packageName))) {
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
                WhitelistEnforcer.LOG.log(Level.INFO,
                        "Removed one ore more candidates for requirement {0} as they are not in the correct region", requirement);
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Override
    public void end() {
        // Nothing to do
    }
}
