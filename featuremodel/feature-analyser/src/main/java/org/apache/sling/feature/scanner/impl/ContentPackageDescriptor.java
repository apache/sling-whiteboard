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
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.scanner.ArtifactDescriptor;
import org.apache.sling.feature.scanner.BundleDescriptor;

/**
 * Information about a content package.
 */
public class ContentPackageDescriptor extends ArtifactDescriptor {

    /** The content package name. */
    private String name;

    /** Bundles in the content package. */
    public final List<BundleDescriptor> bundles = new ArrayList<>();

    /** Configurations in the content package. */
    public final List<Configuration> configs = new ArrayList<>();

    private File artifactFile;

    private Artifact artifact;

    /**
     * Get the artifact file
     * @return The artifact file
     */
    @Override
    public File getArtifactFile() {
        return artifactFile;
    }

    /**
     * Get the artifact
     * @return The artifact
     */
    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    /**
     * Set the artifact
     * @param artifact The artifact
     */
    public void setArtifact(Artifact artifact) {
        checkLocked();
        this.artifact = artifact;
    }

    /**
     * Set the artifact file
     * @param artifactFile The artifact file
     */
    public void setArtifactFile(File artifactFile) {
        checkLocked();
        this.artifactFile = artifactFile;
    }

    /** Optional: the artifact of the content package. */
    private Artifact contentPackage;

    /** Optional: the path inside of the content package. */
    private String contentPath;

    /**
     * Get the content package
     * @return The content package or {@code null}
     */
    public Artifact getContentPackage() {
        return contentPackage;
    }

    /**
     * Get the content path
     * @return The content path or {@code null}
     */
    public String getContentPath() {
        return this.contentPath;
    }

    /**
     * Whether this artifact is embedded in a content package
     * @return {@code true} if embedded.
     */
    public boolean isEmbeddedInContentPackage() {
        return this.contentPath != null;
    }

    /**
     * Set the information about the content package containing this artifact
     * @param artifact The package
     * @param path The path inside the package
     */
    public void setContentPackageInfo(final Artifact artifact, final String path) {
        checkLocked();
        this.contentPackage = artifact;
        this.contentPath = path;
    }

    public String getName() {
        return name;
    }

    public void setName(final String value) {
        checkLocked();
        this.name = value;
    }


    public boolean hasEmbeddedArtifacts() {
        return !this.bundles.isEmpty() || !this.configs.isEmpty();
    }

    @Override
    public String toString() {
        return "ContentPackage [" + name + "]";
    }
}

