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
package org.apache.sling.models.persistor.impl;

import com.drew.lang.annotations.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Path;
import org.apache.sling.models.persistor.ModelPersistor;
import org.apache.sling.models.persistor.annotations.DirectDescendants;
import org.apache.sling.models.persistor.impl.util.ReflectionUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.models.persistor.impl.util.ReflectionUtils.getAnnotatedValue;

/**
 * Code to persist a given object graph to a sling resource tree.
 *
 */
@Component(service = ModelPersistor.class)
public class ModelPersistorImpl implements ModelPersistor {

    public ModelPersistorImpl() {
        // Utility class, cannot be instantiated
    }

    /**
     * My private logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelPersistorImpl.class);

    static final Map<String, Object> RESOURCE_TYPE_NT_UNSTRUCTURED = new HashMap<>();

    static final String JCR_PRIMARYTYPE = "jcr:primaryType";
    static final String NT_UNSTRUCTURED = "nt:unstructured";
    static final String JCR_CONTENT = "jcr:content";

    static {
        RESOURCE_TYPE_NT_UNSTRUCTURED.put(JCR_PRIMARYTYPE, NT_UNSTRUCTURED);
    }

    @Override
    public void persist(final @NotNull Object instance, @NotNull ResourceResolver resourceResolver) throws PersistenceException, IllegalArgumentException, IllegalAccessException, RepositoryException {
        persist(instance, resourceResolver, true);
    }

    @Override
    public void persist(final @NotNull Object instance, @NotNull ResourceResolver resourceResolver, boolean deepPersist) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        String path = getJcrPath(instance);
        persist(path, instance, resourceResolver, deepPersist);
    }

    @Override
    public void persist(final @NotNull String nodePath, final @NotNull Object instance,
            @NotNull ResourceResolver resourceResolver) throws PersistenceException, IllegalArgumentException, IllegalAccessException, RepositoryException {
        persist(nodePath, instance, resourceResolver, true);
    }

    @Override
    public void persist(final @NotNull String nodePath, final @NotNull Object instance,
            @NotNull ResourceResolver resourceResolver, boolean deepPersist)
            throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        if (StringUtils.isBlank(nodePath)) {
            throw new IllegalArgumentException("Node path cannot be null/empty");
        }

        if (instance == null) {
            throw new IllegalArgumentException("Object to save cannot be null");
        }

        if (resourceResolver == null) {
            throw new IllegalArgumentException("ResourceResolver cannot be null");
        }

        Resource resource;
        final ResourceTypeKey resourceType = ResourceTypeKey.fromObject(instance);

        // let's create the resource first
        LOGGER.debug("Creating node at: {} of type: {}", nodePath, resourceType.nodeType);
        boolean isUpdate = resourceResolver.getResource(nodePath) != null;
        Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:primaryType", resourceType.nodeType);

        resource = ResourceUtil.getOrCreateResource(resourceResolver, nodePath,  properties, NT_UNSTRUCTURED, true);
        if (resourceType.resourceType != null) {
            resource.adaptTo(ModifiableValueMap.class).put("sling:resourceType", resourceType.resourceType);
        }

        if (ReflectionUtils.isArrayOrCollection(instance)) {
            persistComplexValue(instance, true, "dunno", resource);
        } else {
            // find properties to be saved
            List<Field> fields = ReflectionUtils.getAllFields(instance.getClass());
            if (fields == null || fields.isEmpty()) {
                // TODO: remove all properties
            } else {
                Resource r = resource;
                fields.stream()
                        .filter(field -> ReflectionUtils.isNotTransient(field, isUpdate))
                        .filter(field -> ReflectionUtils.isSupportedType(field) || ReflectionUtils.isCollectionOfPrimitiveType(field))
                        // TODO: Be smarter about transient fields declared with Lombok
                        .filter(f -> ReflectionUtils.hasNoTransientGetter(f.getName(), instance.getClass()))
                        .forEach(field -> persistField(r, instance, field, deepPersist));
            }
        }

        // save back
        resourceResolver.commit();
    }

    private void persistField(@NotNull Resource resource, @NotNull Object instance, Field field, boolean deepPersist) {
        try {
            // read the existing resource map
            ModifiableValueMap values = resource.adaptTo(ModifiableValueMap.class);
            String nodePath = resource.getPath();

            // find the type of field
            final Class<?> fieldType = field.getType();
            final String fieldName = ReflectionUtils.getFieldName(field);

            // set accessible
            field.setAccessible(true);

            // handle the value as primitive first
            if (ReflectionUtils.isPrimitiveFieldType(fieldType) || ReflectionUtils.isCollectionOfPrimitiveType(field)) {

                Object value = ReflectionUtils.getStorableValue(field.get(instance));

                // remove the attribute that is null, or remove in case it changes type
                if (!fieldName.equals("jcr:primaryType")) {
                    values.remove(fieldName);
                }
                if (value != null) {
                    values.put(fieldName, value);
                }
            } else if (deepPersist) {
                boolean directDescendents = field.getAnnotation(DirectDescendants.class) != null;
                persistComplexValue(field.get(instance), directDescendents, fieldName, resource);
            }
        } catch (IllegalAccessException | RepositoryException | PersistenceException ex) {
            LOGGER.error("Error when persisting content to " + resource.getPath(), ex);
        }
    }

    private void persistComplexValue(Object obj, Boolean implicitCollection, final String fieldName, Resource resource) throws RepositoryException, IllegalAccessException, IllegalArgumentException, PersistenceException {
        ResourceResolver rr = resource.getResourceResolver();
        String childrenRoot = buildChildrenRoot(resource.getPath(), fieldName, rr, implicitCollection);
        boolean deleteRoot = true;
        if (obj != null) {
            if (Collection.class.isAssignableFrom(obj.getClass())) {
                Collection collection = (Collection) obj;
                if (!collection.isEmpty()) {
                    persistCollection(childrenRoot, collection, rr);
                    deleteRoot = false;
                }
            } else if (Map.class.isAssignableFrom(obj.getClass())) {
                Map map = (Map) obj;
                if (!map.isEmpty()) {
                    persistMap(childrenRoot, map, rr);
                    deleteRoot = false;
                }
            } else {
                // this is a single compound object
                // create a child node and persist all its values
                persist(resource.getPath() + "/" + fieldName, obj, rr, true);
                deleteRoot = false;
            }
            if (deleteRoot) {
                Resource rootNode = rr.getResource(childrenRoot);
                if (rootNode != null) {
                    rr.delete(rootNode);
                }
            }
        }
    }

    private void persistCollection(final String collectionRoot, final Collection collection, ResourceResolver resourceResolver) throws PersistenceException, RepositoryException, IllegalArgumentException, IllegalAccessException {
        // now for each child in the collection - create a new node
        Set<String> childNodes = new HashSet<>();
        if (collection != null) {
            for (Object childObject : collection) {
                String childName = null;
                String childPath = getJcrPath(childObject);

                if (childPath != null) {
                    childName = extractNodeNameFromPath(childPath);
                } else {
                    childName = UUID.randomUUID().toString();
                }

                childPath = collectionRoot + "/" + childName;
                childNodes.add(childPath);
                persist(childPath, childObject, resourceResolver, true);
            }
        }
        deleteOrphanNodes(resourceResolver, collectionRoot, childNodes);
    }

    private <K, V> void persistMap(final String collectionRoot, final Map<K, V> collection, ResourceResolver resourceResolver) throws PersistenceException, RepositoryException, IllegalArgumentException, IllegalAccessException {
        // now for each child in the collection - create a new node
        Set<String> childNodes = new HashSet<>();
        if (collection != null) {
            for (Map.Entry<K, V> childObject : collection.entrySet()) {
                String childName = String.valueOf(childObject.getKey());
                String childPath = collectionRoot + "/" + childName;
                childNodes.add(childPath);
                persist(childPath, childObject.getValue(), resourceResolver, true);
            }
        }
        deleteOrphanNodes(resourceResolver, collectionRoot, childNodes);
    }

    private String buildChildrenRoot(String nodePath, String fieldName, ResourceResolver rr, boolean implicitCollection) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        if (implicitCollection) {
            return nodePath;
        } else {
            // create a child collection of required type
            // and persist all instances

            // create the first child node first
            persist(nodePath + "/" + fieldName, new Object(), rr, false);

            // update collection root
            return nodePath + "/" + fieldName;
        }
    }

    private static void deleteOrphanNodes(ResourceResolver resourceResolver, final String collectionRoot, Set<String> childNodes) {
        // Delete any children that are not in the list
        Resource collectionParent = resourceResolver.getResource(collectionRoot);
        if (collectionParent != null) {
            StreamSupport
                    .stream(resourceResolver.getChildren(collectionParent).spliterator(), false)
                    .filter(r -> !childNodes.contains(r.getPath()))
                    .forEach(r -> {
                        try {
                            resourceResolver.delete(r);
                        } catch (PersistenceException ex) {
                            LOGGER.error("Unable to remove stale resource at " + r.getPath(), ex);
                        }
                    });
        }
    }

    private static String extractNodeNameFromPath(String pathValue) throws IllegalArgumentException, IllegalAccessException {
        int lastIndex = pathValue.lastIndexOf('/');
        if (lastIndex >= 0) {
            pathValue = pathValue.substring(lastIndex + 1);
        }

        return pathValue;
    }

    private static String getJcrPath(Object obj) {
        String path = (String) getAnnotatedValue(obj, Path.class);
        if (path != null) {
            return path;
        }

        try {
            Method pathGetter = MethodUtils.getMatchingMethod(obj.getClass(), "getPath");
            if (pathGetter != null) {
                return (String) MethodUtils.invokeMethod(obj, "getPath");
            }

            Field pathField = FieldUtils.getDeclaredField(obj.getClass(), "path");
            if (pathField != null) {
                return (String) FieldUtils.readField(pathField, obj, true);
            }
        } catch (IllegalArgumentException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            LOGGER.warn("exception caught", ex);
        }
        LOGGER.warn("Object of type {} does NOT contain a Path attribute or a path property - multiple instances may conflict", obj.getClass());
        return null;
    }
}
