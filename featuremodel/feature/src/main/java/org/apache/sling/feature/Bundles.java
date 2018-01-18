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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Bundles groups a list of bundles {@code Artifact} and provides a means
 * to sort them based on start order.
 */
public class Bundles implements Iterable<Artifact> {

    /** The list of bundles. */
    private final List<Artifact> bundles = new ArrayList<>();

    /**
     * Get the start order of a bundle.
     * @param bundle The bundle
     * @return The start order, if no start order is defined, {@code 0} is returned.
     */
    public static int getStartOrder(final Artifact bundle) {
        final String order = bundle.getMetadata().get(Artifact.KEY_START_ORDER);
        final int startOrder;
        if ( order != null ) {
            startOrder = Integer.parseInt(order);
        } else {
            startOrder = 0;
        }

        return startOrder;
    }

    /**
     * Get the map of all bundles sorted by start order. The map is sorted
     * and iterating over the keys is done in start level order.
     * @return The map of bundles. The map is unmodifiable.
     */
    public Map<Integer, List<Artifact>> getBundlesByStartOrder() {
        final Map<Integer, List<Artifact>> startOrderMap = new TreeMap<>(new Comparator<Integer>() {

            @Override
            public int compare(final Integer o1, final Integer o2) {
                if ( o1 == o2 ) {
                    return 0;
                }
                if ( o1 == 0 ) {
                    return 1;
                }
                if ( o2 == 0 ) {
                    return -1;
                }
                return o1-o2;
            }
        });

        for(final Artifact bundle : this.bundles) {
            final int startOrder = getStartOrder(bundle);
            List<Artifact> list = startOrderMap.get(startOrder);
            if ( list == null ) {
                list = new ArrayList<>();
                startOrderMap.put(startOrder, list);
            }
            list.add(bundle);
        }
        return Collections.unmodifiableMap(startOrderMap);
    }

    public List<Map.Entry<Integer, Artifact>> getAllBundles() {
        final List<Map.Entry<Integer, Artifact>> list = new ArrayList<>();
        for(final Artifact a : this) {
            list.add(new Map.Entry<Integer, Artifact>() {

                @Override
                public Artifact setValue(Artifact value) {
                    return null;
                }

                @Override
                public Artifact getValue() {
                    // TODO Auto-generated method stub
                    return a;
                }

                @Override
                public Integer getKey() {
                    return getStartOrder(a);
                }
            });
        }
        return list;
    }

    /**
     * Add an artifact as a bundle.
     * @param bundle The bundle
     */
    public void add(final Artifact bundle) {
        this.bundles.add(bundle);
    }

    /**
     * Remove the exact artifact.
     * All start orders are searched for such an artifact. The first one found is removed.
     * @param id The artifact id
     * @return {@code true} if the artifact has been removed
     */
    public boolean removeExact(final ArtifactId id) {
        for(final Artifact artifact : this.bundles) {
            if ( artifact.getId().equals(id)) {
                this.bundles.remove(artifact);
                return true;
            }
        }
        return false;
    }

    /**
     * Remove the same artifact, neglecting the version.
     * All start orders are searched for such an artifact. The first one found is removed.
     * @param id The artifact id
     * @return {@code true} if the artifact has been removed
     */
    public boolean removeSame(final ArtifactId id) {
        for(final Artifact artifact : this.bundles) {
            if ( artifact.getId().isSame(id)) {
                this.bundles.remove(artifact);
                return true;
            }
        }
        return false;
    }

    /**
     * Clear the bundles list.
     */
    public void clear() {
        this.bundles.clear();
    }

    /**
     * Get start order and artifact for the given id, neglecting the version
     * @param id The artifact id
     * @return A map entry with start order and artifact, {@code null} otherwise
     */
    public Map.Entry<Integer, Artifact> getSame(final ArtifactId id) {
        for(final Artifact bundle : this) {
            if ( bundle.getId().isSame(id)) {
                final int startOrder = getStartOrder(bundle);
                return new Map.Entry<Integer, Artifact>() {

                    @Override
                    public Integer getKey() {
                        return startOrder;
                    }

                    @Override
                    public Artifact getValue() {
                        return bundle;
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
        for(final Artifact entry : this) {
            if ( entry.getId().equals(id)) {
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
        for(final Artifact entry : this) {
            if ( entry.getId().isSame(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Iterate over all bundles
     */
    @Override
    public Iterator<Artifact> iterator() {
        return Collections.unmodifiableList(this.bundles).iterator();
    }

    @Override
    public String toString() {
        return "Bundles " + this.bundles;
    }
}
