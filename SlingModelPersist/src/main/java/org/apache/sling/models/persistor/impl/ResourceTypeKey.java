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
 */package org.apache.sling.models.persistor.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Named;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.persistor.annotations.ResourceType;

import static org.apache.sling.models.persistor.impl.util.ReflectionUtils.getAnnotatedValue;

/**
 * Represents primary/child pair type
 */
public class ResourceTypeKey {

    static final Map<Class<?>, ResourceTypeKey> NODE_TYPE_FOR_CLASS = new HashMap<>();
    static public final ResourceTypeKey NT_UNSTRUCTURED = new ResourceTypeKey("nt:unstructured", null);

    final public String nodeType;
    final public String resourceType;

    public ResourceTypeKey(String nodeType, String resourceType) {
        this.nodeType = nodeType != null ? nodeType : "nt:unstructured";
        this.resourceType = resourceType;
    }

    public static ResourceTypeKey fromObject(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if (obj == null) {
            return ResourceTypeKey.NT_UNSTRUCTURED;
        }

        if (NODE_TYPE_FOR_CLASS.containsKey(obj.getClass())) {
            return NODE_TYPE_FOR_CLASS.get(obj.getClass());
        }

        String nodeType = getNodeType(obj);
        Model modelAnnotation = obj.getClass().getAnnotation(Model.class);
        String resourceType = (String) getAnnotatedValue(obj, ResourceType.class);

        // Use the model annotation for resource type as needed
        if (resourceType == null
                && modelAnnotation != null
                && modelAnnotation.resourceType() != null
                && modelAnnotation.resourceType().length >= 1) {
            resourceType = modelAnnotation.resourceType()[0];
        }

        if (nodeType != null || resourceType != null) {
            ResourceTypeKey key = new ResourceTypeKey(nodeType, resourceType);
            NODE_TYPE_FOR_CLASS.put(obj.getClass(), key);
            return key;
        } else {
            return ResourceTypeKey.NT_UNSTRUCTURED;
        }
    }

    private static String getNodeType(Object obj) throws IllegalArgumentException, IllegalAccessException {
        for (Field f : obj.getClass().getDeclaredFields()) {
            Named namedAnnotation = f.getAnnotation(Named.class);
            if (namedAnnotation != null && namedAnnotation.value().equals("jcr:primaryType")) {
                return String.valueOf(FieldUtils.readField(f, obj, true));
            }
        }
        return null;
    }
}
