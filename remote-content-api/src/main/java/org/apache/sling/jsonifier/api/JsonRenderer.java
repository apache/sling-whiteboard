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

package org.apache.sling.jsonifier.api;

import javax.json.JsonObjectBuilder;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JsonRenderer {
    enum Mode {
        CONTENT,
        NAVIGATION
    };

    String SP_RESOURCE_TYPES = "sling.json.resourceTypes";
    String SP_SELECTORS = "sling.json.selectors";

    /** Render supplied Resource to JSON according to supplied selectors
     *  TODO the target should be a MapOfMaps that can be used for both
     *  JSON serialization and as part of a GraphQL query response
    */
    void render(@NotNull JsonObjectBuilder target, @NotNull Resource r, Mode mode, @Nullable String [] selectors);

    /** Decide whether to recurse into supplied child resource */
    boolean recurseInto(Resource child, Mode mode);
}