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
package org.apache.sling.feature.apiregions.model;

import java.util.Iterator;
import java.util.Stack;

/**
 * <code>api-regions</code> in memory representation.
 */
public final class ApiRegions implements Iterable<ApiRegion> {

    private final Stack<ApiRegion> regions = new Stack<>();

    /**
     * Creates then adds a new API region, given its name.
     *
     * <i>Please note</i>: according the <code>api-regions</code> specifications,
     * the order in which the regions are created influences the regions hierarchy.
     *
     * @param regionName the name of the region to be created, must be not null and not empty.
     * @return the created region, identified by the passed name.
     */
    public ApiRegion addNew(String regionName) {
        if (regionName == null || regionName.isEmpty()) {
            throw new IllegalArgumentException("Impossible to create a new API Region without specifying a valid name");
        }

        ApiRegion parent = regions.isEmpty() ? null : regions.peek(); // null parent means 'root' in the hierarchy
        ApiRegion newRegion = new ApiRegion(regionName, parent);
        return regions.push(newRegion);
    }

    /**
     * Search and returns, if found, the region identified by the given name.
     *
     * @param regionName the name of the region to find
     * @return the region identified by the passed name, null if not found or the name is null or empty
     */
    public ApiRegion getByName(String regionName) {
        if (regionName == null || regionName.isEmpty()) {
            return null;
        }

        for (ApiRegion region : regions) {
            if (regionName.equals(region.getName())) {
                return region;
            }
        }

        return null;
    }

    /**
     * Checks if any region is present
     *
     * @return true if there is at least one declared regin, false otherwise.
     */
    public boolean isEmpty() {
        return regions.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<ApiRegion> iterator() {
        return regions.iterator();
    }

}
