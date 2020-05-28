/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.graphql.samples.website.models;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ReadOnlyFallbackMap<K, V> implements Map<K, V> {

    private final Map<K, V> main;
    private final Map<K, V> fallback;
    private final Set<K> mergedKeys = new HashSet<>();

    private static final String MSG_READONLY = "Read-only Map";
    private static final String MSG_NOT_NEEDED = "Probably not needed for our demo code";


    ReadOnlyFallbackMap(Map<K, V> main, Map<K, V> fallback) {
        this.main = main;
        this.fallback = fallback;
        main.keySet().stream().forEach(mergedKeys::add);
        fallback.keySet().stream().forEach(mergedKeys::add);
    }

    @Override
    public int size() {
        return mergedKeys.size();
    }

    @Override
    public boolean isEmpty() {
        return mergedKeys.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return mergedKeys.contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return main.containsValue(value) || fallback.containsValue(value);
    }

    @Override
    public V get(Object key) {
        V result = main.get(key);
        if(result == null) {
            result = fallback.get(key);
        }
        return result;
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException(MSG_READONLY);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException(MSG_READONLY);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException(MSG_READONLY);
    }

    @Override
    public Set<K> keySet() {
        return mergedKeys;
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException(MSG_NOT_NEEDED);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException(MSG_NOT_NEEDED);
    }
}