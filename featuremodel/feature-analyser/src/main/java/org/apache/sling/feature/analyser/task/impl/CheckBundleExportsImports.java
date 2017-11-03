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
package org.apache.sling.feature.analyser.task.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.sling.feature.analyser.BundleDescriptor;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.support.util.PackageInfo;
import org.osgi.framework.Version;

public class CheckBundleExportsImports implements AnalyserTask {

    @Override
    public String getName() {
        return "Bundle Import/Export Check";
    }

    @Override
    public String getId() {
        return "bundle-packages";
    }

    public static final class Report {

        public List<PackageInfo> exportWithoutVersion = new ArrayList<>();

        public List<PackageInfo> exportMatchingSeveral = new ArrayList<>();

        public List<PackageInfo> importWithoutVersion = new ArrayList<>();

        public List<PackageInfo> missingExports = new ArrayList<>();

        public List<PackageInfo> missingExportsWithVersion = new ArrayList<>();

        public List<PackageInfo> missingExportsForOptional = new ArrayList<>();
    }

    private Report getReport(final Map<BundleDescriptor, Report> reports, final BundleDescriptor info) {
        Report report = reports.get(info);
        if ( report == null ) {
            report = new Report();
            reports.put(info, report);
        }
        return report;
    }

    private void checkForVersionOnExportedPackages(final AnalyserTaskContext ctx, final Map<BundleDescriptor, Report> reports) {
        for(final BundleDescriptor info : ctx.getDescriptor().getBundleDescriptors()) {
            if ( info.getExportedPackages() != null ) {
                for(final PackageInfo i : info.getExportedPackages()) {
                    if ( i.getPackageVersion().compareTo(Version.emptyVersion) == 0 ) {
                        getReport(reports, info).exportWithoutVersion.add(i);
                    }
                }
            }
        }
    }

    private void checkForVersionOnImportingPackages(final AnalyserTaskContext ctx, final Map<BundleDescriptor, Report> reports) {
        for(final BundleDescriptor info : ctx.getDescriptor().getBundleDescriptors()) {
            if ( info.getImportedPackages() != null ) {
                for(final PackageInfo i : info.getImportedPackages()) {
                    if ( i.getVersion() == null ) {
                        // don't report for javax and org.w3c. packages (TODO)
                        if ( !i.getName().startsWith("javax.")
                             && !i.getName().startsWith("org.w3c.")) {
                            getReport(reports, info).importWithoutVersion.add(i);
                        }
                    }
                }
            }
        }
    }


    @Override
    public void execute(final AnalyserTaskContext ctx) throws IOException {
        // basic checks
        final Map<BundleDescriptor, Report> reports = new HashMap<>();
        checkForVersionOnExportedPackages(ctx, reports);
        checkForVersionOnImportingPackages(ctx, reports);

        final SortedMap<Integer, List<BundleDescriptor>> bundlesMap = new TreeMap<>();
        for(final BundleDescriptor bi : ctx.getDescriptor().getBundleDescriptors()) {
            List<BundleDescriptor> list = bundlesMap.get(bi.getBundleStartLevel());
            if ( list == null ) {
                list = new ArrayList<>();
                bundlesMap.put(bi.getBundleStartLevel(), list);
            }
            list.add(bi);
        }

        // add all system packages
        final List<BundleDescriptor> exportingBundles = new ArrayList<>();
        exportingBundles.add(ctx.getDescriptor().getFrameworkDescriptor());

        for(final Map.Entry<Integer, List<BundleDescriptor>> entry : bundlesMap.entrySet()) {
            // first add all exporting bundles
            for(final BundleDescriptor info : entry.getValue()) {
                if ( info.getExportedPackages() != null ) {
                    exportingBundles.add(info);
                }
            }
            // check importing bundles
            for(final BundleDescriptor info : entry.getValue()) {
                if ( info.getImportedPackages() != null ) {
                    for(final PackageInfo pck : info.getImportedPackages() ) {
                        final List<BundleDescriptor> candidates = getCandidates(exportingBundles, pck);
                        if ( candidates.isEmpty() ) {
                            if ( pck.isOptional() ) {
                                getReport(reports, info).missingExportsForOptional.add(pck);
                            } else {
                                getReport(reports, info).missingExports.add(pck);
                            }
                        } else {
                            final List<BundleDescriptor> matchingCandidates = new ArrayList<>();
                            for(final BundleDescriptor i : candidates) {
                                if ( i.isExportingPackage(pck) ) {
                                    matchingCandidates.add(i);
                                }
                            }
                            if ( matchingCandidates.isEmpty() ) {
                                if ( pck.isOptional() ) {
                                    getReport(reports, info).missingExportsForOptional.add(pck);
                                } else {
                                    getReport(reports, info).missingExportsWithVersion.add(pck);
                                }
                            } else if ( matchingCandidates.size() > 1 ) {
                                getReport(reports, info).exportMatchingSeveral.add(pck);
                            }
                        }
                    }
                }
            }
        }

        for(final Map.Entry<BundleDescriptor, Report> entry : reports.entrySet()) {
            final String key = "Bundle " + entry.getKey().getArtifact().getId().getArtifactId() + ":" + entry.getKey().getArtifact().getId().getVersion();

            if ( !entry.getValue().importWithoutVersion.isEmpty() ) {
                ctx.reportWarning(key + " is importing package(s) " + getPackageInfo(entry.getValue().importWithoutVersion, false) + " without specifying a version range.");
            }
            if ( !entry.getValue().exportWithoutVersion.isEmpty() ) {
                ctx.reportWarning(key + " is exporting package(s) " + getPackageInfo(entry.getValue().importWithoutVersion, false) + " without a version.");
            }

            if ( !entry.getValue().missingExports.isEmpty() ) {
                ctx.reportError(key + " is importing package(s) " + getPackageInfo(entry.getValue().missingExports, false) + " in start level " +
                        String.valueOf(entry.getKey().getBundleStartLevel()) + " but no bundle is exporting these for that start level.");
            }
            if ( !entry.getValue().missingExportsWithVersion.isEmpty() ) {
                ctx.reportError(key + " is importing package(s) " + getPackageInfo(entry.getValue().missingExportsWithVersion, true) + " in start level " +
                        String.valueOf(entry.getKey().getBundleStartLevel()) + " but no bundle is exporting these for that start level in the required version range.");
            }
        }
    }

    private String getPackageInfo(final List<PackageInfo> pcks, final boolean includeVersion) {
        if ( pcks.size() == 1 ) {
            if (includeVersion) {
                return pcks.get(0).toString();
            } else {
                return pcks.get(0).getName();
            }
        }
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append('[');
        for(final PackageInfo info : pcks) {
            if ( first ) {
                first = false;
            } else {
                sb.append(", ");
            }
            if (includeVersion) {
                sb.append(info.toString());
            } else {
                sb.append(info.getName());
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private List<BundleDescriptor> getCandidates(final List<BundleDescriptor> exportingBundles, final PackageInfo pck) {
        final List<BundleDescriptor> candidates = new ArrayList<>();
        for(final BundleDescriptor info : exportingBundles) {
            if ( info.isExportingPackage(pck.getName()) ) {
                candidates.add(info);
            }
        }
        return candidates;
    }
}
