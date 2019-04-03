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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class GenericMapComparatorTest {

    private GenericMapComparator comparator;
    @Before
    public void setUp() {
        comparator = new GenericMapComparator("map");
    }

    @After
    public void tearDown() {
        comparator = null;
    }

    @Test(expected = NullPointerException.class)
    public void constructoreRequiresValidId() {
        new GenericMapComparator(null);
    }

    @Test
    public void checkRemoved() {
        Map<String, String> previous = new HashMap<>();
        previous.put("removed", "removed entry");

        Map<String, String> current = new HashMap<>();

        DiffSection mapsDiff = comparator.compare(previous, current);

        assertFalse(mapsDiff.isEmpty());
        assertEquals("removed", mapsDiff.getRemoved().iterator().next());
    }

    @Test
    public void checkAdded() {
        Map<String, String> previous = new HashMap<>();

        Map<String, String> current = new HashMap<>();
        current.put("added", "added entry");

        DiffSection mapsDiff = comparator.compare(previous, current);

        assertFalse(mapsDiff.isEmpty());
        assertEquals("added", mapsDiff.getAdded().iterator().next());
    }

    @Test
    public void checkUpdated() {
        Map<String, String> previous = new HashMap<>();
        previous.put("updated", "regular entry");

        Map<String, String> current = new HashMap<>();
        current.put("updated", "updated entry");

        DiffSection mapsDiff = comparator.compare(previous, current);

        assertFalse(mapsDiff.isEmpty());

        @SuppressWarnings("unchecked") // type known by design
        UpdatedItem<String> updated = (UpdatedItem<String>) mapsDiff.getUpdatedItems().iterator().next();
        assertEquals("updated", updated.getId());
        assertEquals("regular entry", updated.getPrevious());
        assertEquals("updated entry", updated.getCurrent());
    }

}
