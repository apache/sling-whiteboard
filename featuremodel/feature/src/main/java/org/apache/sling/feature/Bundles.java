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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;

/**
 * Bundles groups bundle {@code Artifact}s by start level.
 */
public class Bundles implements Iterable<Map.Entry<Integer, Artifact>> {

    /** Map of bundles grouped by start level */
    private final Map<Integer, List<Artifact>> startLevelMap = new TreeMap<>();

    /**
     * Get the map of all bundles sorted by start level. The map is sorted
     * and iterating over the keys is done in start level order.
     * @return The map of bundles. The map is unmodifiable.
     */
    public Map<Integer, List<Artifact>> getBundlesByStartLevel() {
        return Collections.unmodifiableMap(this.startLevelMap);
    }

    /**
     * Add an artifact in the given start level.
     * @param startLevel The start level
     * @param bundle The bundle
     */
    public void add(final int startLevel, final Artifact bundle) {
        List<Artifact> list = this.startLevelMap.get(startLevel);
        if ( list == null ) {
            list = new ArrayList<>();
            this.startLevelMap.put(startLevel, list);
        }
        list.add(bundle);
    }

    /**
     * Remove the exact artifact.
     * All start levels are searched for such an artifact. The first one found is removed.
     * @param id The artifact id
     * @return {@code true} if the artifact has been removed
     */
    public boolean removeExact(final ArtifactId id) {
        for(final Map.Entry<Integer, List<Artifact>> entry : this.startLevelMap.entrySet()) {
            for(final Artifact artifact : entry.getValue()) {
                if ( artifact.getId().equals(id)) {
                    entry.getValue().remove(artifact);
                    if ( entry.getValue().isEmpty() ) {
                        this.startLevelMap.remove(entry.getKey());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Remove the same artifact, neglecting the version.
     * All start levels are searched for such an artifact. The first one found is removed.
     * @param id The artifact id
     * @return {@code true} if the artifact has been removed
     */
    public boolean removeSame(final ArtifactId id) {
        for(final Map.Entry<Integer, List<Artifact>> entry : this.startLevelMap.entrySet()) {
            for(final Artifact artifact : entry.getValue()) {
                if ( artifact.getId().isSame(id)) {
                    entry.getValue().remove(artifact);
                    if ( entry.getValue().isEmpty() ) {
                        this.startLevelMap.remove(entry.getKey());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Clear the bundles map.
     */
    public void clear() {
        this.startLevelMap.clear();
    }

    /**
     * Get start level and artifact for the given id, neglecting the version
     * @param id The artifact id
     * @return A map entry with start level and artifact, {@code null} otherwise
     */
    public Map.Entry<Integer, Artifact> getSame(final ArtifactId id) {
        for(final Map.Entry<Integer, Artifact> entry : this) {
            if ( entry.getValue().getId().isSame(id)) {
                return new Map.Entry<Integer, Artifact>() {

                    @Override
                    public Integer getKey() {
                        return entry.getKey();
                    }

                    @Override
                    public Artifact getValue() {
                        return entry.getValue();
                    }

                    @Override
                    public Artifact setValue(final Artifact value) {
                        throw new IllegalStateException();
                    }
                };
            }
        }
        return null;
    }

    /**
     * Checks whether the exact artifact is available
     * @param id The artifact id.
     * @return {@code true} if the artifact exists
     */
    public boolean containsExact(final ArtifactId id) {
        for(final Map.Entry<Integer, Artifact> entry : this) {
            if ( entry.getValue().getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the same artifact is available, neglecting the version
     * @param id The artifact id.
     * @return {@code true} if the artifact exists
     */
    public boolean containsSame(final ArtifactId id) {
        for(final Map.Entry<Integer, Artifact> entry : this) {
            if ( entry.getValue().getId().isSame(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Iterate over all bundles
     */
    @Override
    public Iterator<Map.Entry<Integer, Artifact>> iterator() {
        final Iterator<Map.Entry<Integer, List<Artifact>>> mainIter = this.startLevelMap.entrySet().iterator();
        return new Iterator<Map.Entry<Integer,Artifact>>() {

            private Map.Entry<Integer, Artifact> next = seek();

            private Integer level;

            private Iterator<Artifact> innerIter;

            private Map.Entry<Integer, Artifact> seek() {
                Map.Entry<Integer, Artifact> entry = null;
                while ( this.innerIter != null || mainIter.hasNext() ) {
                    if ( innerIter != null ) {
                        if ( innerIter.hasNext() ) {
                            final Artifact a = innerIter.next();
                            final Integer l = this.level;
                            entry = new Map.Entry<Integer, Artifact>() {

                                @Override
                                public Integer getKey() {
                                    return l;
                                }

                                @Override
                                public Artifact getValue() {
                                    return a;
                                }

                                @Override
                                public Artifact setValue(Artifact value) {
                                    throw new UnsupportedOperationException();
                                }
                            };
                            break;
                        } else {
                            innerIter = null;
                        }
                    } else {
                        final Map.Entry<Integer, List<Artifact>> e = mainIter.next();
                        this.level = e.getKey();
                        this.innerIter = e.getValue().iterator();
                    }
                }
                return entry;
            }

            @Override
            public boolean hasNext() {
                return this.next != null;
            }

            @Override
            public Entry<Integer, Artifact> next() {
                final Entry<Integer, Artifact> result = next;
                if ( result == null ) {
                    throw new NoSuchElementException();
                }
                this.next = seek();
                return result;
            }

        };
    }

    @Override
    public String toString() {
        return "Bundles [" + this.startLevelMap
                + "]";
    }
}
