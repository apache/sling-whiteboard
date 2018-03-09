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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Helper class to hold key value pairs.
 */
public class KeyValueMap
    implements Iterable<Map.Entry<String, String>> {

    /** The map holding the actual key value pairs. */
    private final Map<String, Object> properties = new TreeMap<>();

    /**
     * Get an item from the map.
     * @param key The key of the item.
     * @return The item or {@code null}.
     */
    public String get(final String key) {
        Object val = this.properties.get(key);
        if (val instanceof String) {
            return (String) val;
        }
        return null;
    }

    public Object getObject(final String key) {
        return this.properties.get(key);
    }

    /**
     * Put an item in the map
     * @param key The key of the item.
     * @param value The value
     */
    public void put(final String key, final Object value) {
        this.properties.put(key, value);
    }

    /**
     * Remove an item from the map
     * @param key The key of the item.
     * @return The previously stored value for the key or {@code null}.
     */
    public Object remove(final String key) {
        return this.properties.remove(key);
    }

    /**
     * Put all items from the other map in this map
     * @param map The other map
     */
    public void putAll(final KeyValueMap map) {
        this.properties.putAll(map.properties);
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        // TODO hack
        Map<String, String> copied = new TreeMap<>();
        for (Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getValue() instanceof String) {
                copied.put(entry.getKey(), (String) entry.getValue());
            }
        }
        return copied.entrySet().iterator();
    }

    /**
     * Check whether this map is empty.
     * @return {@code true} if the map is empty.
     */
    public boolean isEmpty() {
        return this.properties.isEmpty();
    }

    @Override
    public String toString() {
        return properties.toString();
    }

    /**
     * Get the size of the map.
     * @return The size of the map.
     */
    public int size() {
        return this.properties.size();
    }

    /**
     * Clear the map
     */
    public void clear() {
        this.properties.clear();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KeyValueMap other = (KeyValueMap) obj;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        return true;
    }
}
