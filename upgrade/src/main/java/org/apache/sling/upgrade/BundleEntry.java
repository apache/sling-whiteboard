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
package org.apache.sling.upgrade;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a bundle entry loaded from a Sling JAR. Contains the bundle
 * manifest data, start level, bundle contents and installation requirements.
 */
public class BundleEntry implements Comparable<BundleEntry> {
    private static final String BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";

    private static final Pattern ENTRY_PATTERN = Pattern.compile("resources\\/install\\/\\d{1,2}\\/[\\w\\-\\.]+\\.jar");

    private static final Logger log = LoggerFactory.getLogger(BundleEntry.class);

    public static boolean matches(JarEntry entry) {
        return ENTRY_PATTERN.matcher(entry.getName()).matches();
    }

    private final byte[] contents;

    private final boolean installed;

    private final int start;

    private final String symbolicName;

    private final boolean updated;
    private final Version version;

    public BundleEntry(JarEntry entry, InputStream is, BundleContext bundleContext) throws IOException {
        String startStr = entry.getName().split("\\/")[2];
        start = Integer.parseInt(startStr, 10);
        log.debug("Loaded start level {}", start);

        log.debug("Reading bundle contents");
        contents = IOUtils.toByteArray(is);
        log.debug("Loaded {} bytes", contents.length);
        try (JarInputStream bundleIs = new JarInputStream(new ByteArrayInputStream(contents))) {

            Manifest manifest = bundleIs.getManifest();
            Attributes attributes = manifest.getMainAttributes();

            if (attributes.getValue(BUNDLE_SYMBOLIC_NAME).contains(";")) {
                symbolicName = StringUtils.substringBefore(attributes.getValue(BUNDLE_SYMBOLIC_NAME), ";");
            } else {
                symbolicName = attributes.getValue(BUNDLE_SYMBOLIC_NAME);
            }
            log.debug("Loaded symbolic name: {}", symbolicName);

            version = new Version(attributes.getValue("Bundle-Version"));
            log.debug("Loaded version: {}", version);
        }

        Bundle currentBundle = null;
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(this.symbolicName)) {
                currentBundle = b;
            }
        }
        if (currentBundle != null) {
            installed = true;
            if (currentBundle.getVersion().compareTo(version) < 0) {
                updated = true;
            } else {
                updated = false;
            }
        } else {
            installed = false;
            updated = true;
        }
    }

    @Override
    public int compareTo(BundleEntry o) {
        if (this.start != o.start) {
            return this.start - o.start;
        } else {
            return symbolicName.compareTo(o.symbolicName);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BundleEntry other = (BundleEntry) obj;
        if (symbolicName == null) {
            if (other.symbolicName != null)
                return false;
        } else if (!symbolicName.equals(other.symbolicName))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    /**
     * @return the contents
     */
    public byte[] getContents() {
        return contents;
    }

    public int getStart() {
        return start;
    }

    /**
     * @return the symbolicName
     */
    public String getSymbolicName() {
        return symbolicName;
    }

    /**
     * @return the version
     */
    public Version getVersion() {
        return version;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((symbolicName == null) ? 0 : symbolicName.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    /**
     * @return the installed
     */
    public boolean isInstalled() {
        return installed;
    }

    /**
     * @return the updated
     */
    public boolean isUpdated() {
        return updated;
    }

}
