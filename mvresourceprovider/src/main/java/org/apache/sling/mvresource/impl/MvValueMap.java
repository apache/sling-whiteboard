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
package org.apache.sling.mvresource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ValueMap;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.StreamStore;

public class MvValueMap implements ValueMap, ModifiableValueMap {

    private MVMap<String, Object> map;
    private StreamStore store;

    public MvValueMap(MVMap<String, Object> map, StreamStore binaryStore) {
        this.map = map;
        this.store = binaryStore;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        Object value = map.get(key);
        if (value instanceof String) {
            String stringValue = value.toString();
            if (stringValue.startsWith("{b}")) {
                return store.get(stringValue.substring(3).getBytes());
            }
        }
        return value;
    }

    @Override
    public Object put(String key, Object value) {
        if (value instanceof InputStream) {
            try {
                value = "{b}"+ new String(store.put((InputStream)value));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return map.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        map.putAll(map);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Object> values() {
        return map.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String name, Class<T> type) {
        Object response = get(name);
        if (response == null) {
            return null;
        }
        if (!response.getClass().isAssignableFrom(type)) {
            return null;
        }
        return (T)response;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String name, T defaultValue) {
        return (T) map.getOrDefault(name, defaultValue);
    }
    

}
