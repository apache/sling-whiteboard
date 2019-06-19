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
package org.apache.sling.feature.diff.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.feature.diff.impl.FrameworkPropertiesComparator;
import org.junit.Test;

public class FrameworkPropertiesComparatorTest extends AbstractComparatorTest<FrameworkPropertiesComparator> {

    @Override
    protected FrameworkPropertiesComparator newComparatorInstance() {
        return new FrameworkPropertiesComparator();
    }

    @Test
    public void checkRemoved() {
        Map<String, String> previous = new HashMap<>();
        previous.put("removed", "removed entry");

        Map<String, String> current = new HashMap<>();

        comparator.computeDiff(previous, current, targetFeature);

        assertFalse(targetFeature.getPrototype().getFrameworkPropertiesRemovals().isEmpty());
        assertFalse(targetFeature.getFrameworkProperties().containsKey("removed"));
    }

    @Test
    public void checkAdded() {
        Map<String, String> previous = new HashMap<>();

        Map<String, String> current = new HashMap<>();
        current.put("added", "added entry");

        comparator.computeDiff(previous, current, targetFeature);

        assertTrue(targetFeature.getPrototype().getFrameworkPropertiesRemovals().isEmpty());
        assertFalse(targetFeature.getFrameworkProperties().isEmpty());
        assertTrue(targetFeature.getFrameworkProperties().containsKey("added"));
    }

    @Test
    public void checkUpdated() {
        Map<String, String> previous = new HashMap<>();
        previous.put("updated", "regular entry");

        Map<String, String> current = new HashMap<>();
        current.put("updated", "updated entry");

        comparator.computeDiff(previous, current, targetFeature);

        assertTrue(targetFeature.getPrototype().getFrameworkPropertiesRemovals().isEmpty());
        assertFalse(targetFeature.getFrameworkProperties().isEmpty());
        assertTrue(targetFeature.getFrameworkProperties().containsKey("updated"));
        assertEquals("updated entry", targetFeature.getFrameworkProperties().get("updated"));
    }

}
