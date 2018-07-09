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
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

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

        long reqBundleID = requirement.getRevision().getBundle().getBundleId();

        try {
            Features fs = featureServiceTracker.waitForService(SERVICE_WAIT_TIMEOUT);

            // The Feature Service could not be found, skip candidate pruning
            if (fs == null) {
                WhitelistEnforcer.LOG.warn("Could not obtain the feature service, no whitelist enforcement");
                return;
            }

            String reqFeat = fs.getFeatureForBundle(reqBundleID);
            Set<String> regions = whitelistService.listRegions(reqFeat);

            nextCapability:
            for (Iterator<BundleCapability> it = candidates.iterator(); it.hasNext(); ) {
                BundleCapability bc = it.next();

                BundleRevision rev = bc.getRevision();

                // A bundle is allowed to wire to itself
                long capBundleID = rev.getBundle().getBundleId();
                if (capBundleID == reqBundleID)
                    continue nextCapability;

                String capFeat = fs.getFeatureForBundle(capBundleID);

                // Within a single feature everything can wire to everything else
                if (reqFeat.equals(capFeat))
                    continue nextCapability;


                Object pkg = bc.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                if (pkg instanceof String) {
                    String packageName = (String) pkg;
                    // If the export is in the global region.
                    if (Boolean.TRUE.equals(whitelistService.regionWhitelistsPackage(WhitelistService.GLOBAL_REGION, packageName)))
                        continue nextCapability;

                    // If the export is in a region that the feature is also in, then allow
                    for (String region : regions) {
                        // We've done this one already
                        if (WhitelistService.GLOBAL_REGION.equals(region))
                            continue;

                        if (!Boolean.FALSE.equals(whitelistService.regionWhitelistsPackage(region, packageName)))
                            continue nextCapability;
                    }

                    // The capability package is not visible by the requirer
                    // remove from the candidates.
                    it.remove();
                }
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
