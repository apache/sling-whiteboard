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
 */package org.apache.sling.models.persist.impl;

import java.util.HashMap;
import java.util.Map;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.persist.annotations.ChildType;
import org.apache.sling.models.persist.annotations.ResourceType;

import static org.apache.sling.models.persist.impl.util.ReflectionUtils.getAnnotatedValue;

/**
 * Represents primary/child pair type
 */
public class ResourceTypeKey {

    static final Map<Class<?>, ResourceTypeKey> NODE_TYPE_FOR_CLASS = new HashMap<>();
    static public final ResourceTypeKey NT_UNSTRUCTURED = new ResourceTypeKey("nt:unstructured", null);

    final public String primaryType;
    final public String childType;

    public ResourceTypeKey(String primaryType, String childType) {
        this.primaryType = primaryType;
        this.childType = childType;
    }

    public static ResourceTypeKey fromObject(Object obj) {
        if (obj == null) {
            return ResourceTypeKey.NT_UNSTRUCTURED;
        }

        if (NODE_TYPE_FOR_CLASS.containsKey(obj.getClass())) {
            return NODE_TYPE_FOR_CLASS.get(obj.getClass());
        }

        Model modelAnnotation = obj.getClass().getAnnotation(Model.class);
        String primaryType = (String) getAnnotatedValue(obj, ResourceType.class);
        // Use the model annotation for resource type as needed
        if (primaryType == null
                && modelAnnotation != null
                && modelAnnotation.resourceType() != null
                && modelAnnotation.resourceType().length == 1) {
            primaryType = modelAnnotation.resourceType()[0];
        }
        String childType = (String) getAnnotatedValue(obj, ChildType.class);
        if (primaryType != null) {
            ResourceTypeKey key = new ResourceTypeKey(primaryType, childType);
            NODE_TYPE_FOR_CLASS.put(obj.getClass(), key);
            return key;
        } else {
            return ResourceTypeKey.NT_UNSTRUCTURED;
        }
    }
}
