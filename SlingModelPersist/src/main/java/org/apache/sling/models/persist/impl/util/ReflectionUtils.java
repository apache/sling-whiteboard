/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.apache.sling.models.persist.impl.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.persist.annotations.Ignore;
import org.apache.sling.models.persist.ModelPersist;

/**
 * Utility methods around object reflection.
 *
 */
public class ReflectionUtils {

    private ReflectionUtils() {
        // Utility class cannot be instantiated
    }

    /**
     * Classes that correspond to basic parameter types, which are handled
     * directly in code
     */
    static final Set<Class<?>> BASIC_PARAMS = new HashSet<>();

    static final Set<Class<?>> UNSUPPORTED_CLASSES = new HashSet<>();
    static final Set<String> UNSUPPORTED_PACKAGES = new HashSet<>();

    /**
     * Add default values
     */
    static {
        // primitives
        BASIC_PARAMS.add(byte.class);
        BASIC_PARAMS.add(char.class);
        BASIC_PARAMS.add(short.class);
        BASIC_PARAMS.add(int.class);
        BASIC_PARAMS.add(long.class);
        BASIC_PARAMS.add(float.class);
        BASIC_PARAMS.add(double.class);
        BASIC_PARAMS.add(boolean.class);

        // primitive boxed
        BASIC_PARAMS.add(Byte.class);
        BASIC_PARAMS.add(Character.class);
        BASIC_PARAMS.add(Short.class);
        BASIC_PARAMS.add(Integer.class);
        BASIC_PARAMS.add(Long.class);
        BASIC_PARAMS.add(Float.class);
        BASIC_PARAMS.add(Double.class);
        BASIC_PARAMS.add(Boolean.class);

        // other basic types
        BASIC_PARAMS.add(String.class);
        BASIC_PARAMS.add(Calendar.class);
        BASIC_PARAMS.add(Date.class);
        BASIC_PARAMS.add(URI.class);
        BASIC_PARAMS.add(BigDecimal.class);

        // basic type arrays
        BASIC_PARAMS.add(String[].class);
        BASIC_PARAMS.add(Calendar[].class);
        BASIC_PARAMS.add(Date[].class);
        BASIC_PARAMS.add(URI[].class);
        BASIC_PARAMS.add(BigDecimal[].class);

        // primitives array
        BASIC_PARAMS.add(byte[].class);
        BASIC_PARAMS.add(char[].class);
        BASIC_PARAMS.add(short[].class);
        BASIC_PARAMS.add(int[].class);
        BASIC_PARAMS.add(long[].class);
        BASIC_PARAMS.add(float[].class);
        BASIC_PARAMS.add(double[].class);
        BASIC_PARAMS.add(boolean[].class);

        // primitive boxed arrays
        BASIC_PARAMS.add(Byte[].class);
        BASIC_PARAMS.add(Character[].class);
        BASIC_PARAMS.add(Short[].class);
        BASIC_PARAMS.add(Integer[].class);
        BASIC_PARAMS.add(Long[].class);
        BASIC_PARAMS.add(Float[].class);
        BASIC_PARAMS.add(Double[].class);
        BASIC_PARAMS.add(Boolean[].class);

        // Any field with this type will be ignored
        UNSUPPORTED_CLASSES.add(Resource.class);
        UNSUPPORTED_CLASSES.add(ModelPersist.class);
        UNSUPPORTED_PACKAGES.add("javax.jcr");
        UNSUPPORTED_PACKAGES.add("com.day.cq");
        UNSUPPORTED_PACKAGES.add("org.apache.sling.api");
        UNSUPPORTED_PACKAGES.add("com.adobe.acs.commons.mcp");
    }

    public static Collection<Class<?>> getSupportedPropertyTypes() {
        return BASIC_PARAMS;
    }

    public static boolean isArrayOrCollection(Object instance) {
        return instance.getClass().isArray() || instance instanceof Collection;
    }

    /**
     * Check if a given field is transient. A field is considered transient if
     * and only if the field is marked with `transient` keyword and no
     * annotation of type {@link Named} exists over the field; or if the field
     * is marked with {@link Ignore} annotation.
     *
     * @param field the non-<code>null</code> field to check.
     *
     * @return <code>false</code> if field is to be considered transient,
     * <code>true</code> otherwise
     */
    public static boolean isNotTransient(Field field) {
        if (field != null && Modifier.isTransient(field.getModifiers())) {
            // if property is covered using @Named annotation it shall not be excluded
            Named aemProperty = field.getAnnotation(Named.class);
            return aemProperty != null;
        } else {
            // is the property annotated with @Ignore?
            return field == null || field.getAnnotation(Ignore.class) == null;
        }
    }

    public static boolean hasNoTransientGetter(String fieldName, Class clazz) {
        PropertyDescriptor desc;
        try {
            desc = new PropertyDescriptor(fieldName, clazz);
            if (desc.getReadMethod() != null && desc.getReadMethod().getAnnotation(Transient.class) != null) {
                return false;
            }
        } catch (IntrospectionException ex) {
            // Do nothing
        }
        return true;
    }

    public static boolean isSupportedType(Field field) {
        Class clazz = field.getType();
        if (Map.class.isAssignableFrom(clazz)) {
            ParameterizedType p = (ParameterizedType) field.getGenericType();
            Type paramType = p.getActualTypeArguments()[1];
            try {
                // In case the value type is a collection of something, check to be safe
                if (!Class.class.isAssignableFrom(paramType.getClass())) {
                    paramType = ((ParameterizedType) paramType).getActualTypeArguments()[0];
                }
                // Assume for now that we've narrowed down to the final object type to confirm
                clazz = (Class) paramType;
            } catch (ClassCastException ex) {
                return false;
            }
        }
        if (UNSUPPORTED_CLASSES.contains(clazz)) {
            return false;
        }
        Package pkg = clazz.isArray() ? clazz.getComponentType().getPackage() : clazz.getPackage();
        if (pkg == null) {
            return true;
        } else {
            String packageName = pkg.getName();
            return UNSUPPORTED_PACKAGES
                    .stream()
                    .noneMatch(packageName::startsWith);
        }
    }

    /**
     * Return all fields including all-private and all-inherited fields for the
     * given class.
     *
     * @param clazz the class for which fields are needed
     *
     * @return the {@link List} of {@link Field} objects in no certain order
     *
     * @throws IllegalArgumentException if given class is <code>null</code>
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        populateAllFields(clazz, fields);
        return fields;
    }

    public static void populateAllFields(Class<?> clazz, List<Field> fields) {
        if (clazz == null) {
            return;
        }

        Field[] array = clazz.getDeclaredFields();
        if (AssertUtils.isNotEmpty(array)) {
            fields.addAll(Arrays.asList(array));
        }

        if (clazz.getSuperclass() == null) {
            return;
        }

        populateAllFields(clazz.getSuperclass(), fields);
    }

    // Utility function common to reading/writing start here
    /**
     * Returns the name of the field to look for in JCR.
     *
     * @param field
     * @return
     */
    public static String getFieldName(Field field) {
        Named namedAnnotation = field.getAnnotation(Named.class);
        Via viaAnnotation = field.getAnnotation(Via.class);
        if (namedAnnotation != null) {
            return namedAnnotation.value();
        } else if (viaAnnotation != null && viaAnnotation.value() != null) {
            return viaAnnotation.value();
        } else {
            return field.getName();
        }
    }

    public static boolean isPrimitiveFieldType(Class<?> fieldType) {
        return getSupportedPropertyTypes().contains(fieldType);
    }

    /**
     * Get the value defined on an annotation, if it is a class annotation, or a
     * method or field-level member which has that annotation
     *
     * @param obj Object which has the given annotation as a class, method, or
     * field annotation
     * @param annotatedType desired annotation type class
     * @return Value if found otherwise null
     */
    public static Object getAnnotatedValue(Object obj, Class annotatedType) {
        if (obj == null) {
            return null;
        }
        Annotation a = obj.getClass().getAnnotation(annotatedType);
        try {
            if (a != null) {
                String value = (String) MethodUtils.invokeMethod(a, "value");
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            // Do nothing, it didn't have a value defined on that annotation
        }
        List<Field> fields = FieldUtils.getFieldsListWithAnnotation(obj.getClass(), annotatedType);
        try {
            if (fields == null || fields.isEmpty()) {
                List<Method> methods = MethodUtils.getMethodsListWithAnnotation(obj.getClass(), annotatedType);
                return CollectionUtils.isNotEmpty(methods) ? methods.get(0).invoke(obj) : null;
            } else {
                return fields.get(0).get(obj);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            return null;
        }
    }
}
