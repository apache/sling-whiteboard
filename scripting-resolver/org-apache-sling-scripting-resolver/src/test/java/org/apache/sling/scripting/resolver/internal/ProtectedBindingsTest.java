/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.resolver.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProtectedBindingsTest {

    @Test
    public void put() {
        ProtectedBindings bindings = getBindingsUnderTest();
        bindings.put("d", 4);
        boolean allowsOverride;
        try {
            bindings.put("c", 0);
            allowsOverride = true;
        } catch (IllegalArgumentException e) {
            allowsOverride = false;
        }
        assertFalse("Protected keys were overwritten.", allowsOverride);
        assertEquals("Expected that unprotected keys can be used.", 4, bindings.get("d"));
    }

    @Test
    public void putAll() {
        ProtectedBindings bindings = getBindingsUnderTest();
        boolean allowsOverride;
        try {
            bindings.putAll(new HashMap<String, Object>(){{
                put("a", 0);
                put("b", 0);
                put("c", 0);
            }});
            allowsOverride = true;
        } catch (IllegalArgumentException e) {
            allowsOverride = false;
        }
        assertFalse("Protected keys were overwritten", allowsOverride);
        bindings.putAll(new HashMap<String, Object>(){{
            put("d", 4);
            put("e", 5);
            put("f", 6);
        }});
        assertTrue("Expected that the bindings contain non-conflicting additions.", bindings.size() == 6 && bindings.get("d").equals(4)
                && bindings.get("e").equals(5) && bindings.get("f").equals(6));
    }

    @Test
    public void remove() {
        ProtectedBindings bindings = getBindingsUnderTest();
        boolean allowsOverride;
        try {
            bindings.remove("a");
            allowsOverride = true;
        } catch (IllegalArgumentException e) {
            allowsOverride = false;
        }
        assertFalse("Protected keys were overwritten.", allowsOverride);
        assertEquals("Expected that protected key was not removed.", 1, bindings.get("a"));
        bindings.put("d", 4);
        assertEquals(4, bindings.size());
        assertEquals(4, bindings.remove("d"));
    }

    @Test
    public void clear() {
        ProtectedBindings bindings = getBindingsUnderTest();
        boolean hasThrownUOE = false;
        try {
            bindings.clear();
        } catch (UnsupportedOperationException e) {
            hasThrownUOE = true;
        }
        assertTrue("Expected an UnsupportedOperationException.", hasThrownUOE);
        assertTrue("Expected map to not be modified.", bindings.size() == 3 && bindings.get("a").equals(1) && bindings.get("b").equals(2)
                && bindings.get("c").equals(3));
    }

    @Test
    public void containsValue() {
        Bindings bindings = mock(Bindings.class);
        ProtectedBindings protectedBindings = new ProtectedBindings(bindings, Collections.emptySet());
        protectedBindings.containsValue(4);
        verify(bindings).containsValue(4);
    }

    @Test
    public void entrySet() {
        ProtectedBindings bindings = getBindingsUnderTest();
        Set<Map.Entry<String, Object>> entrySet = bindings.entrySet();
        assertEquals(3, entrySet.size());
        boolean unmodifiable;
        try {
            entrySet.clear();
            unmodifiable = false;
        } catch (UnsupportedOperationException e) {
            unmodifiable = true;
        }
        assertTrue("Expected an unmodifiable entry set.", unmodifiable);
    }

    @Test
    public void isEmpty() {
        Bindings bindings = mock(Bindings.class);
        ProtectedBindings protectedBindings = new ProtectedBindings(bindings, Collections.emptySet());
        protectedBindings.isEmpty();
        verify(bindings).isEmpty();
    }

    @Test
    public void keySet() {
        ProtectedBindings bindings = getBindingsUnderTest();
        Set<String> keySet = bindings.keySet();
        assertEquals(3, keySet.size());
        boolean unmodifiable;
        try {
            keySet.clear();
            unmodifiable = false;
        } catch (UnsupportedOperationException e) {
            unmodifiable = true;
        }
        assertTrue("Expected an unmodifiable key set.", unmodifiable);
    }

    @Test
    public void size() {
        Bindings bindings = mock(Bindings.class);
        ProtectedBindings protectedBindings = new ProtectedBindings(bindings, Collections.emptySet());
        protectedBindings.size();
        verify(bindings).size();
    }

    @Test
    public void values() {
        ProtectedBindings bindings = getBindingsUnderTest();
        Collection<Object> values = bindings.values();
        assertEquals(3, values.size());
        boolean unmodifiable;
        try {
            values.clear();
            unmodifiable = false;
        } catch (UnsupportedOperationException e) {
            unmodifiable = true;
        }
        assertTrue("Expected an unmodifiable values collection.", unmodifiable);
    }

    @Test
    public void containsKey() {
        Bindings bindings = mock(Bindings.class);
        ProtectedBindings protectedBindings = new ProtectedBindings(bindings, Collections.emptySet());
        protectedBindings.containsKey("a");
        verify(bindings).containsKey("a");
    }

    @Test
    public void get() {
        Bindings bindings = mock(Bindings.class);
        ProtectedBindings protectedBindings = new ProtectedBindings(bindings, Collections.emptySet());
        protectedBindings.get("a");
        verify(bindings).get("a");
    }

    private ProtectedBindings getBindingsUnderTest() {
        Bindings bindings = new SimpleBindings();
        bindings.put("a", 1);
        bindings.put("b", 2);
        bindings.put("c", 3);
        Set<String> protectedKeys = new HashSet<>(Arrays.asList("a", "b", "c"));
        return new ProtectedBindings(bindings, protectedKeys);
    }
}
