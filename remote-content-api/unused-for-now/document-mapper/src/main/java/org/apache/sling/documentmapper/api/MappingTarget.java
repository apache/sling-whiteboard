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

package org.apache.sling.documentmapper.api;

import org.apache.sling.api.adapter.Adaptable;
import org.jetbrains.annotations.NotNull;

/**
 * A target for content mapping, usually implemented by a Map of Maps, a JSON
 * document tree, etc.
 */
public interface MappingTarget {
    String TARGET_TYPE = "sling.mapping.target.type";

    interface TargetNode extends Adaptable {
        TargetNode addChild(String name);
        TargetNode addValue(String name, Object value);
        TargetNode addValue(String name, Object [] value);
        void close();
    }

    @NotNull TargetNode newTargetNode();
}