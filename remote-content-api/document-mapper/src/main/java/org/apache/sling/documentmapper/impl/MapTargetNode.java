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

package org.apache.sling.documentmapper.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.documentmapper.api.MappingTarget;
import org.apache.sling.documentmapper.api.MappingTarget.TargetNode;

/** A TargetNode that outputs to a Map document */
public class MapTargetNode extends HashMap<String, Object> implements MappingTarget.TargetNode {

    MapTargetNode(String name) {
    }

    @Override
    public TargetNode addChild(String name) {
        final MapTargetNode child = new MapTargetNode(name);
        put(name, child);
        return child;
    }

    @Override
    public TargetNode addValue(String name, Object value) {
        put(name, value);
        return this;
    }

    @Override
    public TargetNode addValue(String name, Object[] value) {
        put(name, value);
        return this;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if(type.equals(Map.class)) {
            return (AdapterType)this;
        }
        throw new IllegalArgumentException("For now, can only adapt to a Map");
    }

    @Override
    public void close() {
        // nothing to close
    }
}