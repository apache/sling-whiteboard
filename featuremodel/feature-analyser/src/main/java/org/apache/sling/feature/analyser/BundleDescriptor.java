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
package org.apache.sling.feature.analyser;

import java.util.jar.Manifest;

import org.apache.sling.feature.analyser.impl.BundleDescriptorImpl;
import org.apache.sling.feature.support.util.PackageInfo;

/**
 * Information about a bundle
 */
public abstract class BundleDescriptor extends ArtifactDescriptor implements Comparable<BundleDescriptor> {

    /**
     * Get the bundle symbolic name.
     * @return The bundle symbolic name
     */
    public abstract String getBundleSymbolicName();

    /**
     * Get the bundle version
     * @return The bundle version
     */
    public abstract String getBundleVersion();

    /**
     * Get the start level
     * @return The start level.
     */
    public abstract int getBundleStartLevel();

    /**
     * If the artifact has a manifest, return it
     * @return The manifest
     */
    public abstract Manifest getManifest();

    public boolean isExportingPackage(final String packageName) {
        for(final PackageInfo i : getExportedPackages()) {
            if ( i.getName().equals(packageName) ) {
                return true;
            }
        }
        return false;
    }

    public boolean isExportingPackage(final PackageInfo info) {
        for(final PackageInfo i : getExportedPackages()) {
            if ( i.getName().equals(info.getName())
                 && (info.getVersion() == null || info.getPackageVersionRange().includes(i.getPackageVersion()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof BundleDescriptorImpl ) {
            return this.getBundleSymbolicName().equals(((BundleDescriptorImpl)obj).getBundleSymbolicName()) && this.getBundleVersion().equals(((BundleDescriptorImpl)obj).getBundleVersion());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (this.getBundleSymbolicName() + ':' + this.getBundleVersion()).hashCode();

    }

    @Override
    public String toString() {
        return "BundleInfo [symbolicName=" + getBundleSymbolicName() + ", version=" + this.getBundleVersion() + "]";
    }

    @Override
    public int compareTo(final BundleDescriptor o) {
        return (this.getBundleSymbolicName() + ':' + this.getBundleVersion()).compareTo((o.getBundleSymbolicName() + ':' + o.getBundleVersion()));
    }
}