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
package org.apache.sling.models.persist.impl;

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
import java.util.logging.Level;
import java.util.stream.StreamSupport;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Path;
import org.apache.sling.models.persist.annotations.DirectDescendants;
import org.apache.sling.models.persist.impl.util.AssertUtils;
import org.apache.sling.models.persist.impl.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;


import org.jetbrains.annotations.NotNull;

import static org.apache.sling.models.persist.impl.util.ReflectionUtils.getAnnotatedValue;

import org.apache.sling.models.persist.ModelPersist;

/**
 * Code to persist a given object instance to a JCR node.
 *
 */
@Component(service =ModelPersist.class)
public class ModelPersistImpl implements ModelPersist {

    public ModelPersistImpl() {
        // Utility class, cannot be instantiated
    }

    /**
     * My private logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelPersistImpl.class);

    static final Map<String, Object> RESOURCE_TYPE_NT_UNSTRUCTURED = new HashMap<>();

    static final String JCR_PRIMARYTYPE = "jcr:primaryType";
    static final String NT_UNSTRUCTURED = "nt:unstructured";
    static final String JCR_CONTENT = "jcr:content";

    static {
        RESOURCE_TYPE_NT_UNSTRUCTURED.put(JCR_PRIMARYTYPE, NT_UNSTRUCTURED);
    }

    public  void persist(final @NotNull Object instance, @NotNull ResourceResolver resourceResolver) throws PersistenceException, IllegalArgumentException, IllegalAccessException, RepositoryException {
        persist (instance, resourceResolver, true);
    }
    
    public  void persist(final @NotNull Object instance, @NotNull ResourceResolver resourceResolver, boolean deepPersist) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        String path = getJcrPath(instance);
        persist(path, instance, resourceResolver, deepPersist);
    }
    
    public  void persist(final @NotNull String nodePath, final @NotNull Object instance,
            @NotNull ResourceResolver resourceResolver) throws PersistenceException, IllegalArgumentException, IllegalAccessException, RepositoryException {
        persist (nodePath, instance, resourceResolver, true);
    }
    

    public  void persist(final @NotNull String nodePath, final @NotNull Object instance,
            @NotNull ResourceResolver resourceResolver, boolean deepPersist) 
                    throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        if (nodePath == null || nodePath.trim().isEmpty()) {
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
        LOGGER.debug("Creating node at: {} of type: {}", nodePath, resourceType.primaryType);
        resource = ResourceUtil.getOrCreateResource(resourceResolver, nodePath, resourceType.primaryType, NT_UNSTRUCTURED, true);
        if (AssertUtils.isNotEmpty(resourceType.childType)) {
            LOGGER.debug("Needs a child node, creating node at: {} of type: {}", nodePath, resourceType.childType);
            resource = ResourceUtil.getOrCreateResource(resourceResolver, nodePath + "/" + JCR_CONTENT, resourceType.childType, NT_UNSTRUCTURED, true);
        }

        if (ReflectionUtils.isArrayOrCollection(instance)) {
            persistComplexValue(instance, true, nodePath, "dunno", resource);
        } else {
            // find properties to be saved
            List<Field> fields = ReflectionUtils.getAllFields(instance.getClass());
            if (fields == null || fields.isEmpty()) {
                // TODO: remove all properties
            } else {
                Resource r = resource;
                fields.stream()
                        .filter(ReflectionUtils::isNotTransient)
                        .filter(ReflectionUtils::isSupportedType)
                        .filter(f -> ReflectionUtils.hasNoTransientGetter(f.getName(), instance.getClass()))
                        .forEach(field -> persistField(r, instance, field, deepPersist));
            }
        }

        // save back
        resourceResolver.commit();
    }

    private  void persistField(@NotNull Resource resource, @NotNull Object instance, Field field, boolean deepPersist) {
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
            if (ReflectionUtils.isPrimitiveFieldType(fieldType)) {
                Object value = field.get(instance);

                // remove the attribute that is null, or remove in case it changes type
                values.remove(fieldName);
                if (value != null) {
                    values.put(fieldName, value);
                }
            } else if (deepPersist) {
                boolean directDescendents = field.getAnnotation(DirectDescendants.class) != null;
                persistComplexValue(field.get(instance), directDescendents, nodePath, fieldName, resource);
            }
        } catch (IllegalAccessException | RepositoryException | PersistenceException ex) {
            LOGGER.error("Error when persisting content to " + resource.getPath(), ex);
        }
    }

    private  void persistComplexValue(Object obj, Boolean implicitCollection, String nodePath, final String fieldName, Resource resource) throws RepositoryException, IllegalAccessException, IllegalArgumentException, PersistenceException {
        if (obj == null) {
            return;
        }
        if (Collection.class.isAssignableFrom(obj.getClass())) {
            Collection collection = (Collection) obj;
            if (!collection.isEmpty()) {
                String childrenRoot = buildChildrenRoot(nodePath, fieldName, resource.getResourceResolver(), implicitCollection);
                persistCollection(childrenRoot, collection, resource.getResourceResolver());
            }
        } else if (Map.class.isAssignableFrom(obj.getClass())) {
            Map map = (Map) obj;
            if (!map.isEmpty()) {
                String childrenRoot = buildChildrenRoot(nodePath, fieldName, resource.getResourceResolver(), implicitCollection);
                persistMap(childrenRoot, map, resource.getResourceResolver());
            }
        } else {
            // this is a single compound object
            // create a child node and persist all its values
            persist(nodePath + "/" + fieldName, obj, resource.getResourceResolver(), true);
        }
    }

    private  void persistCollection(final String collectionRoot, final Collection collection, ResourceResolver resourceResolver) throws PersistenceException, RepositoryException, IllegalArgumentException, IllegalAccessException {
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

    private  <K, V> void persistMap(final String collectionRoot, final Map<K,V> collection, ResourceResolver resourceResolver) throws PersistenceException, RepositoryException, IllegalArgumentException, IllegalAccessException {
        // now for each child in the collection - create a new node
        Set<String> childNodes = new HashSet<>();
        if (collection != null) {
            for (Map.Entry<K,V> childObject : collection.entrySet()) {
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
            LOGGER.warn("exception caught",ex);
        }
        LOGGER.warn("Object of type {} does NOT contain a Path attribute or a path property - multiple instances may conflict", obj.getClass());
        return null;
    }
}
