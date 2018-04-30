/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.resolver.internal;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LazyBindingsTest {

    private static final String THE_QUESTION = "the answer";
    private static final int THE_ANSWER = 42;

    private Set<String> usedSuppliers;
    private LazyBindings lazyBindings;

    @Before
    public void setUp() {
        usedSuppliers = new HashSet<>();
        final Map<String, Supplier<Object>> supplierMap = new HashMap<>();
        supplierMap.put(THE_QUESTION, () -> {
            usedSuppliers.add(THE_QUESTION);
            return THE_ANSWER;
        });
        lazyBindings = new LazyBindings(supplierMap);
    }

    @After
    public void tearDown() {
        usedSuppliers = null;
        lazyBindings = null;
    }

    @Test
    public void testLazyGet() {
        assertFalse(usedSuppliers.contains(THE_QUESTION));
        assertEquals(THE_ANSWER, lazyBindings.get(THE_QUESTION));
        assertTrue(usedSuppliers.contains(THE_QUESTION));
        assertNull(lazyBindings.get("none"));
    }

    @Test
    public void testLazyContainsKey() {
        lazyBindings.put("a", 0);
        assertTrue(lazyBindings.containsKey(THE_QUESTION));
        assertTrue(lazyBindings.containsKey("a"));
        assertFalse(usedSuppliers.contains(THE_QUESTION));

    }

    @Test
    public void testLazyEntrySet() {
        lazyBindings.put("a", 0);
        Set<Map.Entry<String, Object>> expectedEntrySet = new HashSet<>();
        expectedEntrySet.add(new AbstractMap.SimpleEntry<>(THE_QUESTION, THE_ANSWER));
        expectedEntrySet.add(new AbstractMap.SimpleEntry<>("a", 0));
        assertFalse(usedSuppliers.contains(THE_QUESTION));
        assertEquals(expectedEntrySet, lazyBindings.entrySet());
        assertTrue(usedSuppliers.contains(THE_QUESTION));
    }

    @Test
    public void testLazyKeySet() {
        lazyBindings.put("a", 0);
        assertEquals(new HashSet<>(Arrays.asList(THE_QUESTION, "a")), lazyBindings.keySet());
        assertFalse(usedSuppliers.contains(THE_QUESTION));
    }

    @Test
    public void testLazyGetOrDefault() {
        lazyBindings.put("a", 0);
        assertEquals(0, lazyBindings.getOrDefault("a", 1));
        assertFalse(usedSuppliers.contains(THE_QUESTION));
        assertEquals(THE_ANSWER, lazyBindings.getOrDefault(THE_QUESTION, THE_ANSWER + 1));
        assertTrue(usedSuppliers.contains(THE_QUESTION));
        assertEquals(1, lazyBindings.getOrDefault("b", 1));
    }





}
