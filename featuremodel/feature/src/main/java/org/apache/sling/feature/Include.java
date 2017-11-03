/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A include is an inclusion of a feature with optional removals of
 * <ul>
 *   <li>Configurations / configuration properties
 *   <li>Bundles
 *   <li>Framework properties
 *   <li>Extensions or artifacts from extensions
 * </ul>
 *
 *  TODO - requirement, capabilities
 */
public class Include implements Comparable<Include> {

    private final ArtifactId id;

    private final List<String> configurationRemovals = new ArrayList<>();

    private final List<ArtifactId> bundleRemovals = new ArrayList<>();

    private final List<String> frameworkPropertiesRemovals = new ArrayList<>();

    private final List<String> extensionRemovals = new ArrayList<>();

    private final Map<String, List<ArtifactId>> artifactExtensionRemovals = new HashMap<>();

    /**
     * Construct a new Include.
     * @param id The id of the feature.
     * @throws IllegalArgumentException If id is {@code null}.
     */
    public Include(final ArtifactId id) {
        if ( id == null ) {
            throw new IllegalArgumentException("id must not be null.");
        }
        this.id = id;
    }

    /**
     * Get the id of the artifact.
     * @return The id.
     */
    public ArtifactId getId() {
        return this.id;
    }

    public List<String> getConfigurationRemovals() {
        return configurationRemovals;
    }

    public List<ArtifactId> getBundleRemovals() {
        return bundleRemovals;
    }

    public List<String> getFrameworkPropertiesRemovals() {
        return frameworkPropertiesRemovals;
    }

    public List<String> getExtensionRemovals() {
        return extensionRemovals;
    }

    public Map<String, List<ArtifactId>> getArtifactExtensionRemovals() {
        return artifactExtensionRemovals;
    }

    @Override
    public int compareTo(final Include o) {
        return this.id.compareTo(o.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return this.id.equals(((Include)obj).id);
    }

    @Override
    public String toString() {
        return "Include [id=" + id.toMvnId()
                + "]";
    }
}
