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
package org.apache.sling.feature.analyser.impl;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Capability;
import org.apache.sling.feature.Requirement;
import org.apache.sling.feature.analyser.BundleDescriptor;
import org.apache.sling.feature.support.util.ManifestParser;
import org.apache.sling.feature.support.util.ManifestUtil;
import org.apache.sling.feature.support.util.PackageInfo;
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

        this.manifest = ManifestUtil.getManifest(file);
        if ( this.manifest == null ) {
            throw new IOException("File has no manifest");
        }
        this.analyze();
        this.lock();
    }

    public BundleDescriptorImpl(final Artifact artifact,
            final Set<PackageInfo> pcks,
            final Set<Requirement> reqs,
            final Set<Capability> caps) throws IOException {
        this.artifact = artifact;
        this.artifactFile = null;
        this.startLevel = 0;

        this.symbolicName = Constants.SYSTEM_BUNDLE_SYMBOLICNAME;
        this.bundleVersion = artifact.getId().getOSGiVersion().toString();
        this.getExportedPackages().addAll(pcks);
        this.getRequirements().addAll(reqs);
        this.getCapabilities().addAll(caps);
        this.manifest = null;
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

            this.getExportedPackages().addAll(ManifestUtil.extractExportedPackages(this.manifest));
            this.getImportedPackages().addAll(ManifestUtil.extractImportedPackages(this.manifest));
            this.getDynamicImportedPackages().addAll(ManifestUtil.extractDynamicImportedPackages(this.manifest));
            try {
                ManifestParser parser = new ManifestParser(this.manifest);
                this.getCapabilities().addAll(ManifestUtil.extractCapabilities(parser));
                this.getRequirements().addAll(ManifestUtil.extractRequirements(parser));
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        } else {
            throw new IOException("Unable to get bundle symbolic name from artifact " + getArtifact().getId().toMvnId());
        }
    }
}