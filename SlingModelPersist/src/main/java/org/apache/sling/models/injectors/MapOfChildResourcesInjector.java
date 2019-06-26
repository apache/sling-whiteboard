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
package org.apache.sling.models.injectors;

import com.drew.lang.annotations.NotNull;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.EnumUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.persistor.annotations.DirectDescendants;
import org.apache.sling.models.spi.AcceptsNullName;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.service.component.annotations.Component;

@Component(service = Injector.class)
public class MapOfChildResourcesInjector implements Injector, AcceptsNullName {

    @Override
    public @NotNull
    String getName() {
        return "map-of-child-resources";
    }

    @Override
    public Object getValue(@NotNull Object adaptable, String name, @NotNull Type declaredType, @NotNull AnnotatedElement element,
            @NotNull DisposalCallbackRegistry callbackRegistry) {
        if (adaptable instanceof Resource) {
            boolean directDescendants = element.getAnnotation(DirectDescendants.class) != null;
            Resource source = ((Resource) adaptable);
            if (!directDescendants) {
                source = source.getChild(name);
            }
            return createMap(source != null ? source.getChildren() : Collections.EMPTY_LIST, declaredType);
        } else if (adaptable instanceof SlingHttpServletRequest) {
            return getValue(((SlingHttpServletRequest) adaptable).getResource(), name, declaredType, element, callbackRegistry);
        }
        return null;
    }

    // TODO: Clean up and refactor this method
    private Object createMap(Iterable<Resource> children, Type declaredType) {
        if (declaredType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) declaredType;
            if (type.getRawType().equals(Map.class)) {
                Type keyType = type.getActualTypeArguments()[0];
                Type valueType = type.getActualTypeArguments()[1];
                if (keyType.equals(String.class) && valueType instanceof Class) {
                    Class<?> valueClass = (Class) valueType;
                    return StreamSupport.stream(children.spliterator(), false).map(r ->
                        buildSimpleMapEntry(valueClass, r)
                    ).filter(o -> o != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
                }
            } else if (type.getRawType().equals(EnumMap.class)) {
                Class keyType = (Class) type.getActualTypeArguments()[0];
                Map<String, Object> enumKeys = EnumUtils.getEnumMap(keyType);
                Type valueType = type.getActualTypeArguments()[1];
                Boolean isSingleValue = valueType instanceof Class;
                Class<?> valueClass = isSingleValue
                        ? (Class) valueType
                        : (Class) ((ParameterizedType) valueType).getActualTypeArguments()[0];
                EnumMap out = new EnumMap(keyType);
                StreamSupport.stream(children.spliterator(), false).map(r
                        -> buildEnumMapEntry(enumKeys, r, valueClass, isSingleValue)
                )
                        .filter(o -> o != null)
                        .forEach(e -> out.put(e.getKey(), e.getValue()));
                return out;
            }
        }
        return null;
    }

    private Map.Entry buildSimpleMapEntry(Class<?> valueClass, Resource r) {
        if (valueClass.equals(Resource.class)) {
            return new AbstractMap.SimpleEntry<>(r.getName(), r);
        } else {
            Object adapted = r.adaptTo(valueClass);
            if (adapted == null) {
                return null;
            } else {
                return new AbstractMap.SimpleEntry<>(r.getName(), adapted);
            }
        }
    }

    private Map.Entry buildEnumMapEntry(Map<String, Object> enumKeys, Resource r, Class<?> valueClass, Boolean isSingleValue) {
        Object key = enumKeys.get(r.getName());
        if (valueClass.equals(Resource.class)) {
            return new AbstractMap.SimpleEntry<>(key, r);
        } else if (isSingleValue) {
            Object adapted = r.adaptTo(valueClass);
            if (adapted == null) {
                return null;
            } else {
                return new AbstractMap.SimpleEntry<>(key, adapted);
            }
        } else {
            List values = StreamSupport.stream(r.getChildren().spliterator(), false)
                    .map(c -> c.adaptTo(valueClass))
                    .collect(Collectors.toList());
            return new AbstractMap.SimpleEntry<>(key, values);
        }
    }
}
