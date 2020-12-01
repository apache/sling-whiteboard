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

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.sling.experimental.typesystem.Type;
import org.apache.sling.experimental.typesystem.Annotation;

/** TODO should not be needed if we tweak the type system APIs just the right way */
class TypeUtil {
    private TypeUtil() {
    }

    static boolean hasAnnotation(Type t, String name) {
        final Set<Annotation> a = (t == null ? null : t.getAnnotations());
        if(a != null) {
            return a.stream().anyMatch(it -> name.equals(it.getName()));
        }
        return false;
    }

    static String getAnnotationValue(Type t, String name) {
        final Set<Annotation> a = (t == null ? null : t.getAnnotations());
        if(a != null) {
            final Optional<Annotation> opt = a.stream().filter(it -> name.equals(it.getName())).findFirst();
            return opt.isPresent() ? opt.get().getValue() : null;
        }
        return null;
    }

    static Pattern getAnnotationPattern(Type t, String name) {
        final String str = getAnnotationValue(t, name);
        return str == null ? null : Pattern.compile(str);
    }
}