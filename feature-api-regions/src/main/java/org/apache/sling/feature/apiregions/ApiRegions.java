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
package org.apache.sling.feature.apiregions;

import java.util.Iterator;
import java.util.Stack;

public final class ApiRegions implements Iterable<ApiRegion> {

    private final Stack<ApiRegion> regions = new Stack<>();

    public ApiRegion addNewRegion(String regionName) {
        if (regionName == null || regionName.isEmpty()) {
            throw new IllegalArgumentException("Impossible to create a new API Region without specifying a valid name");
        }

        ApiRegion parent = regions.isEmpty() ? null : regions.peek(); // null parent means 'root' in the hierarchy
        ApiRegion newRegion = new ApiRegion(regionName, parent);
        return regions.push(newRegion);
    }

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

    public boolean isEmpty() {
        return regions.isEmpty();
    }

    @Override
    public Iterator<ApiRegion> iterator() {
        return regions.iterator();
    }

}
