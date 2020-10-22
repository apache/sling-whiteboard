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

package org.apache.sling.jsonifier.impl;

import javax.json.JsonObjectBuilder;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.jsonifier.api.JsonRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;

@Component(
    service = JsonRenderer.class,
    property = {
        JsonRenderer.SP_RESOURCE_TYPES + "=" + JsonRendererSelectorImpl.DEFAULT_RESOURCE_TYPE,
    })
class DefaultJsonRenderer implements JsonRenderer {

    @Override
    public void render(@NotNull JsonObjectBuilder target, @NotNull Resource r, Mode mode,
        @Nullable String[] selectors) {
        target.add("source", getClass().getName());
    }

    @Override
    public boolean recurseInto(Resource child, Mode mode) {
        return true;
    }
}