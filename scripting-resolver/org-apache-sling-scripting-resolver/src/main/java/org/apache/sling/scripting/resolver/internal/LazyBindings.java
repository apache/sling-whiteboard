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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.script.SimpleBindings;

class LazyBindings extends SimpleBindings {

    private final Map<String, Supplier<Object>> suppliers;

    LazyBindings(Map<String, Supplier<Object>> suppliers) {
        this.suppliers = suppliers;
    }

    @Override
    public Object get(Object key) {
        if (!super.containsKey(key) && suppliers.containsKey(key)) {
            Object value = suppliers.get(key).get();
            put((String) key, value);
        }
        return super.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key) || suppliers.containsKey(key);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entrySet = new HashSet<>(super.entrySet());
        for (Map.Entry<String, Supplier<Object>> supplierEntry : suppliers.entrySet()) {
            entrySet.add(new AbstractMap.SimpleEntry<>(supplierEntry.getKey(), supplierEntry.getValue().get()));
        }
        return Collections.unmodifiableSet(entrySet);
    }

    @Override
    public Set<String> keySet() {
        Set<String> keySet = new HashSet<>(super.keySet());
        if (!suppliers.isEmpty()) {
            keySet.addAll(suppliers.keySet());
        }
        return Collections.unmodifiableSet(keySet);
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        if (!super.containsKey(key) && suppliers.containsKey(key)) {
            Object value = suppliers.get(key).get();
            put((String) key, value);
        }
        return super.getOrDefault(key, defaultValue);
    }
}
