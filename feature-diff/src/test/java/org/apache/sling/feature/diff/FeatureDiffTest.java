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
package org.apache.sling.feature.diff;

import static org.apache.sling.feature.diff.FeatureDiff.loadComparators;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.feature.diff.impl.FeatureElementComparator;
import org.junit.Test;

public final class FeatureDiffTest {

    @Test
    public void loadAllComparators() {
        Set<String> comparators = filterComparators(new DiffRequest());

        assertTrue(comparators.isEmpty());
    }

    @Test
    public void loadIncludedComparators() {
        Set<String> comparators = filterComparators(new DiffRequest()
                                                    .addIncludeComparator("bundles")
                                                    .addIncludeComparator("configurations"));

        assertFalse(comparators.isEmpty());
        assertFalse(comparators.contains("bundles"));
        assertFalse(comparators.contains("configurations"));
        assertTrue(comparators.contains("extensions"));
        assertTrue(comparators.contains("framework-properties"));
    }

    @Test
    public void loadExcludedComparators() {
        Set<String> comparators = filterComparators(new DiffRequest()
                                                    .addExcludeComparator("bundles")
                                                    .addExcludeComparator("configurations"));

        assertFalse(comparators.isEmpty());
        assertTrue(comparators.contains("bundles"));
        assertTrue(comparators.contains("configurations"));
        assertFalse(comparators.contains("extensions"));
        assertFalse(comparators.contains("framework-properties"));
    }

    private Set<String> filterComparators(DiffRequest diffRequest) {
        Set<String> comparators = new HashSet<>();
        comparators.add("bundles");
        comparators.add("configurations");
        comparators.add("extensions");
        comparators.add("framework-properties");

        for (FeatureElementComparator comparator : loadComparators(diffRequest)) {
            comparators.remove(comparator.getId());
        }

        return comparators;
    }

}
