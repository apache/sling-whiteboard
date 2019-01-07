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
package org.apache.sling.modeling.attributes.commons.impl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.modeling.ModelException;
import org.apache.sling.modeling.attributes.AttributeDefinition;
import org.apache.sling.modeling.attributes.AttributeDefinitions;
import org.apache.sling.modeling.attributes.AttributesProvider;
import org.apache.sling.modeling.attributes.annotations.Attribute;
import org.apache.sling.modeling.attributes.annotations.TypeQualifier;
import org.apache.sling.modeling.attributes.annotations.Url;
import org.apache.sling.modeling.attributes.commons.WritableAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class SimpleAttributes<T extends AttributesProvider> implements WritableAttributes<T> {
    private ValueMap vm = new ValueMapDecorator(new LinkedHashMap<>());

    @NotNull
    private AttributeDefinitionsImpl definitions = new AttributeDefinitionsImpl();

    public SimpleAttributes(@NotNull Class<T> annotationSource) throws ModelException {
        defineAttributes(annotationSource);
    }

    @Override
    @Nullable
    public <U> U get(@NotNull String name, @NotNull Class<U> type) {
        return vm.get(name, type);
    }

    @Override
    @NotNull
    public <U> U get(@NotNull String name, @NotNull U defaultValue) {
        return vm.get(name, defaultValue);
    }

    @Override
    public boolean containsName(@NotNull String name) {
        return vm.containsKey(name);
    }

    @Override
    @NotNull
    public AttributeDefinitions getDefinitions() {
        return definitions;
    }

    @Override
    public WritableAttributes<T> put(@NotNull String name, @Nullable Object value) {
        vm.put(name, value);
        return this;
    }

    @SuppressWarnings("null")
    private void defineAttributes(@NotNull Class<T> annotationSource) throws ModelException {
        for (Class<?> superClass : annotationSource.getInterfaces()) {
            if (AttributesProvider.class.isAssignableFrom(superClass)) {
                @SuppressWarnings("unchecked")
                Class<T> superClass2 = (Class<T>) superClass;
                defineAttributes(superClass2);
            }
        }

        try {
            BeanInfo bean = Introspector.getBeanInfo(annotationSource);

            Arrays.stream(bean.getPropertyDescriptors())
                .map(SimpleAttributes::getDefinition)
                .filter(Objects::nonNull)
                .forEach(this::defineAttribute);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private void defineAttribute(AttributeDefinition attr) {
        definitions.map.put(attr.getName(), attr);
    }

    @Nullable
    private static AttributeDefinition getDefinition(@NotNull PropertyDescriptor prop) throws ModelException {
        Method readMethod = prop.getReadMethod();

        @NotNull
        Attribute[] attrs = readMethod.getAnnotationsByType(Attribute.class);

        if (attrs.length == 0) {
            return null;
        }

        Attribute attr = attrs[0];

        String name = getName(attr, prop);

        Class<?> typeClass = prop.getPropertyType();

        boolean required = !Optional.class.isAssignableFrom(typeClass);

        boolean multiple = false;
        if (typeClass.isArray()) {
            multiple = true;
            typeClass = typeClass.getComponentType();
        } else if (Collection.class.isAssignableFrom(typeClass)) {
            multiple = true;
            typeClass = (Class<?>) ((ParameterizedType) readMethod.getGenericReturnType()).getActualTypeArguments()[0];
        } else if (Optional.class.isAssignableFrom(typeClass)) {
            typeClass = (Class<?>) ((ParameterizedType) readMethod.getGenericReturnType()).getActualTypeArguments()[0];
        }

        for (Annotation a : readMethod.getAnnotations()) {
            if (a.annotationType().getAnnotationsByType(TypeQualifier.class).length == 0) {
                continue;
            }

            if (a instanceof Url) {
                Url url = (Url) a;
                return new UrlDefinition(name)
                    .withMultiple(multiple).withRequired(required)
                    .withAbsolute(url.absolute()).withTemplated(url.templated());
            }
        }

        if (typeClass.isAssignableFrom(boolean.class) || typeClass.isAssignableFrom(Boolean.class)) {
            return new BooleanDefinition(name).withMultiple(multiple).withRequired(required);
        } else if (typeClass.isAssignableFrom(long.class) || typeClass.isAssignableFrom(Long.class)) {
            return new NumberDefinition(name).withMultiple(multiple).withRequired(required);
        } else if (typeClass.isAssignableFrom(String.class)) {
            return new StringDefinition(name).withMultiple(multiple).withRequired(required);
        }

        throw new ModelException("Attribute definition of the class not found: " + typeClass);
    }

    @SuppressWarnings("null")
    @NotNull
    private static String getName(@NotNull Attribute attr, @NotNull PropertyDescriptor prop) {
        String name = attr.value();
        if (name.isEmpty()) {
            name = prop.getName();
        }
        return name;
    }

    private static class AttributeDefinitionsImpl implements AttributeDefinitions {

        private Map<String, AttributeDefinition> map = new LinkedHashMap<>();

        @SuppressWarnings("null")
        @Override
        @NotNull
        public AttributeDefinition get(@NotNull String name) {
            return map.get(name);
        }

        @SuppressWarnings("null")
        @Override
        @NotNull
        public Collection<AttributeDefinition> getAll() {
            return Collections.unmodifiableCollection(map.values());
        }
    }
}
