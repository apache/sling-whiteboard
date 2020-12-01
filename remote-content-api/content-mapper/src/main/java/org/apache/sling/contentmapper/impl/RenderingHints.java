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

package org.apache.sling.contentmapper.impl;

import java.util.regex.Pattern;

import org.apache.sling.experimental.typesystem.Type;
import static org.apache.sling.contentmapper.api.AnnotationNames.RENDER_HINT_EXCLUDE_PROPERTY_REGEXP;
import static org.apache.sling.contentmapper.api.AnnotationNames.RENDER_HINT_INCLUDE_PROPERTY_REGEXP;

class RenderingHints {
    private final Pattern includeProperty;
    private final Pattern excludeProperty;

    RenderingHints(Type t) {
        includeProperty = TypeUtil.getAnnotationPattern(t, RENDER_HINT_INCLUDE_PROPERTY_REGEXP);
        excludeProperty = TypeUtil.getAnnotationPattern(t, RENDER_HINT_EXCLUDE_PROPERTY_REGEXP);
    }

    boolean renderProperty(String name) {
        if(includeProperty != null && includeProperty.matcher(name).matches()) {
            return true;
        } else if(excludeProperty != null && excludeProperty.matcher(name).matches()) {
            return false;
        }
        return true;
    }
}