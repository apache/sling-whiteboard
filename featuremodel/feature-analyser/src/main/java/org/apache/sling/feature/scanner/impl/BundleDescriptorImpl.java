/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.scanner.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.resource.ResourceBuilder;
import org.apache.felix.utils.resource.ResourceImpl;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.PackageInfo;
import org.osgi.framework.Constants;

/**
 * Information about a bundle
 */
public class BundleDescriptorImpl
    extends BundleDescriptor {

    /** The bundle symbolic name. */
    private String symbolicName;

    /** The bundle version. */
    private String bundleVersion;

    /** The start level of this artifact. */
    private final int startLevel;

    /** Manifest */
    private final Manifest manifest;

    /** The physical file for analyzing. */
    private final File artifactFile;

    /** The corresponding artifact from the feature. */
    private final Artifact artifact;

    public BundleDescriptorImpl(final Artifact a,
            final File file,
            final int startLevel) throws IOException  {
        this.artifact = a;
        this.artifactFile = file;
        this.startLevel = startLevel;
        try (final JarFile jarFile = new JarFile(this.artifactFile) ) {
            this.manifest = jarFile.getManifest();
        }
        if ( this.manifest == null ) {
            throw new IOException("File has no manifest");
        }
        this.analyze();
        this.lock();
    }

    /**
     * Get the bundle symbolic name.
     * @return The bundle symbolic name
     */
    @Override
    public String getBundleSymbolicName() {
        return symbolicName;
    }

    /**
     * Get the bundle version
     * @return The bundle version
     */
    @Override
    public String getBundleVersion() {
        return bundleVersion;
    }

    /**
     * Get the start level
     * @return The start level or {@code 0} for the default.
     */
    @Override
    public int getBundleStartLevel() {
        return startLevel;
    }

    @Override
    public File getArtifactFile() {
        return artifactFile;
    }

    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    @Override
    public Manifest getManifest() {
        return this.manifest;
    }

    protected void analyze() throws IOException {
        final String name = this.manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        if ( name != null ) {
            final String version = this.manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
            if ( version == null ) {
                throw new IOException("Unable to get bundle version from artifact " + getArtifact().getId().toMvnId());
            }
            this.symbolicName = name;
            this.bundleVersion = version;
            final String newBundleName = this.getArtifact().getMetadata().get("bundle:rename-bsn");
            if (newBundleName != null) {
                this.symbolicName = newBundleName;
            }

            this.getExportedPackages().addAll(extractExportedPackages(this.manifest));
            this.getImportedPackages().addAll(extractImportedPackages(this.manifest));
            this.getDynamicImportedPackages().addAll(extractDynamicImportedPackages(this.manifest));
            try {
                ResourceImpl resource = ResourceBuilder.build(null, this.manifest.getMainAttributes().entrySet().stream()
                    .collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString())));
                this.getCapabilities().addAll(resource.getCapabilities(null));
                this.getRequirements().addAll(resource.getRequirements(null));
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        } else {
            throw new IOException("Unable to get bundle symbolic name from artifact " + getArtifact().getId().toMvnId());
        }
    }

    public static List<PackageInfo> extractPackages(final Manifest m,
        final String headerName,
        final String defaultVersion,
        final boolean checkOptional) {
        final String pckInfo = m.getMainAttributes().getValue(headerName);
        if (pckInfo != null) {
            final Clause[] clauses = Parser.parseHeader(pckInfo);

            final List<PackageInfo> pcks = new ArrayList<>();
            for(final Clause entry : clauses) {
                Object versionObj = entry.getAttribute("version");
                final String version;
                if ( versionObj == null ) {
                    version = defaultVersion;
                } else {
                    version = versionObj.toString();
                }

                boolean optional = false;
                if ( checkOptional ) {
                    final String resolution = entry.getDirective("resolution");
                    optional = "optional".equalsIgnoreCase(resolution);
                }
                final PackageInfo pck = new PackageInfo(entry.getName(),
                    version,
                    optional);
                pcks.add(pck);
            }

            return pcks;
        }
        return Collections.emptyList();
    }

    public static List<PackageInfo> extractExportedPackages(final Manifest m) {
        return extractPackages(m, Constants.EXPORT_PACKAGE, "0.0.0", false);
    }

    public static List<PackageInfo> extractImportedPackages(final Manifest m) {
        return extractPackages(m, Constants.IMPORT_PACKAGE, null, true);
    }

    public static List<PackageInfo> extractDynamicImportedPackages(final Manifest m) {
        return extractPackages(m, Constants.DYNAMICIMPORT_PACKAGE, null, false);
    }
}
