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
package org.apache.sling.models.persist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.StreamSupport;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.persist.bean.BeanWithAnnotatedPathField;
import org.apache.sling.models.persist.bean.BeanWithAnnotatedPathGetter;
import org.apache.sling.models.persist.bean.BeanWithMappedNames;
import org.apache.sling.models.persist.bean.BeanWithPathField;
import org.apache.sling.models.persist.bean.BeanWithPathGetter;
import org.apache.sling.models.persist.bean.ComplexBean;
import org.apache.sling.models.persist.bean.MappedChildren;
import org.apache.sling.models.persist.impl.ModelPersistImpl;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * Test basic JCR persistence behaviors
 */
public class ModelPersistTest {

    @Rule
    public final SlingContext context = new SlingContext();

    ResourceResolver rr;
    ModelPersist jcrPersist = new ModelPersistImpl();

    @Before
    public void setUp() {
        rr = context.resourceResolver();
        context.addModelsForClasses(
                BeanWithAnnotatedPathField.class,
                BeanWithAnnotatedPathGetter.class,
                BeanWithPathField.class,
                BeanWithPathGetter.class,
                BeanWithMappedNames.class,
                BeanWithMappedNames.ChildBean.class,
                MappedChildren.class,
                MappedChildren.Child.class,
                ComplexBean.class,
                ComplexBean.Level2Bean.class,
                ComplexBean.Level3Bean.class);
    }

    /**
     * Confirm that content is written to correct path indicated by either path
     * field, path getter, or path annotation.Also asserts that path annotation
     * takes precedence over any field or getter method.
     *
     * @throws javax.jcr.RepositoryException
     * @throws org.apache.sling.api.resource.PersistenceException
     * @throws java.lang.IllegalAccessException
     */
    @Test
    public void testPersistBeanPath() throws RepositoryException, PersistenceException, IllegalAccessException {
        BeanWithPathGetter bean1 = new BeanWithPathGetter();
        jcrPersist.persist(bean1, rr, false);
        Resource res = rr.getResource(bean1.getPath());
        assertNotNull("Resource not created at expected path", res);
        assertEquals("Expected property not found", bean1.prop1, res.getValueMap().get("prop1", "missing"));

        BeanWithPathField bean2 = new BeanWithPathField();
        jcrPersist.persist(bean2, rr, false);
        res = rr.getResource(bean2.path + "/jcr:content");
        assertNotNull("Resource not created at expected path", res);
        assertEquals("Expected property not found", bean2.prop1, res.getValueMap().get("prop1", "missing"));

        BeanWithAnnotatedPathField bean3 = new BeanWithAnnotatedPathField();
        jcrPersist.persist(bean3, rr);
        res = rr.getResource(bean3.correctPath);
        assertNotNull("Resource not created at expected path", res);
        assertEquals("Expected property not found", bean3.prop1, res.getValueMap().get("prop1", "missing"));

        BeanWithAnnotatedPathGetter bean4 = new BeanWithAnnotatedPathGetter();
        jcrPersist.persist(bean4, rr, true);
        res = rr.getResource(bean4.getCorrectPath());
        assertNotNull("Resource not created at expected path", res);
        assertEquals("Expected property not found", bean4.prop1, res.getValueMap().get("prop1", "missing"));
    }

    /**
     * Confirm that content is persisted at provided path even if it has a path
     * annotation or path getter, etc.
     *
     * @throws javax.jcr.RepositoryException
     * @throws org.apache.sling.api.resource.PersistenceException
     * @throws java.lang.IllegalAccessException
     */
    @Test
    public void testPersistProvidedPath() throws RepositoryException, PersistenceException, IllegalAccessException {
        String testPath = "/manual/path";
        BeanWithAnnotatedPathField bean = new BeanWithAnnotatedPathField();
        jcrPersist.persist(testPath, bean, rr, false);
        Resource res = rr.getResource(bean.correctPath);
        assertNull("Should not have stored content here", res);
        res = rr.getResource(testPath);
        assertNotNull("Resource not created at expected path", res);
        assertEquals("Expected property not found", bean.prop1, res.getValueMap().get("prop1", "missing"));
    }

    @Test
    public void testComplexObjectGraph() throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        // First create a bean with a complex structure and various object types buried in it
        ComplexBean sourceBean = new ComplexBean();
        sourceBean.name = "Complex-bean-test";
        sourceBean.arrayOfStrings = new String[]{"Value 1", "Value 2", "Value 3"};
        sourceBean.level2.name = "Complex-bean-level2";
        ComplexBean.Level3Bean l31 = new ComplexBean.Level3Bean();
        l31.value1 = "L3-1";
        l31.value2 = 123;
        l31.valueList = new String[]{"L31a", "L31b", "L31c", "L31d"};
        ComplexBean.Level3Bean l32 = new ComplexBean.Level3Bean();
        l32.value1 = "L3-2";
        l32.value2 = 456;
        l32.valueList = new String[]{"L32a", "L32b", "L32c", "L32d"};
        l32.path = "/test/complex-beans/Complex-bean-test/level2/level3/child-2";
        sourceBean.level2.level3.add(l31);
        sourceBean.level2.level3.add(l32);

        // Persist the bean
        jcrPersist.persist(sourceBean.getPath(), sourceBean, rr);

        // Now retrieve that object from the repository
        rr.refresh();
        Resource createdResource = rr.getResource(sourceBean.getPath());
        ComplexBean targetBean = createdResource.adaptTo(ComplexBean.class);

        assertNotNull(targetBean);
        assertNotEquals(sourceBean, targetBean);
        assertTrue("Expecing children of object to have been deserialized", targetBean.level2.level3 != null && targetBean.level2.level3.size() > 0);
        targetBean.level2.level3.get(0).path = l31.path;
        assertThat(targetBean).isEqualToComparingFieldByFieldRecursively(sourceBean);
    }

    @Test
    public void testChildObjectRemoval() throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        // First create a bean with a complex structure and various object types buried in it
        ComplexBean sourceBean = new ComplexBean();
        sourceBean.name = "Complex-bean-test";
        sourceBean.arrayOfStrings = new String[]{"Value 1", "Value 2", "Value 3"};
        sourceBean.level2.name = "Complex-bean-level2";
        ComplexBean.Level3Bean l31 = new ComplexBean.Level3Bean();
        l31.value1 = "L3-1";
        l31.value2 = 123;
        l31.valueList = new String[]{"L31a", "L31b", "L31c", "L31d"};
        ComplexBean.Level3Bean l32 = new ComplexBean.Level3Bean();
        l32.value1 = "L3-2";
        l32.value2 = 456;
        l32.valueList = new String[]{"L32a", "L32b", "L32c", "L32d"};
        l32.path = "/test/complex-beans/Complex-bean-test/level2/level3/child-2";
        sourceBean.level2.level3.add(l31);
        sourceBean.level2.level3.add(l32);

        // Persist the bean
        jcrPersist.persist(sourceBean, rr);

        // Child record should exist
        Resource existingResource = rr.getResource(l32.path);
        assertNotNull(existingResource);

        sourceBean.level2.level3.remove(l32);
        jcrPersist.persist(sourceBean, rr);

        // Child record should no longer exist
        Resource deletedResource = rr.getResource(l32.path);
        assertNull(deletedResource);
    }

    @Test
    public void testMappedNames() throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException, JsonProcessingException {
        // Create test beans
        BeanWithMappedNames.ChildBean child1 = new BeanWithMappedNames.ChildBean();
        BeanWithMappedNames.ChildBean child2 = new BeanWithMappedNames.ChildBean();
        BeanWithMappedNames.ChildBean child3 = new BeanWithMappedNames.ChildBean();
        child1.setName("child-1");
        child2.setName("child-2");
        child3.setName("child-3");

        BeanWithMappedNames bean = new BeanWithMappedNames();
        bean.setWrong1("Name");
        bean.setWrong2(new String[]{"foo", "bar", "baz"});
        bean.setWrong3(child1);
        bean.setWrong4(Arrays.asList(child1, child2, child3));
        bean.setWrong5(new HashMap<String, BeanWithMappedNames.ChildBean>() {
            {
                put("child1", child1);
                put("child2", child2);
                put("child3", child3);
            }
        });
        bean.setWrong6(Boolean.TRUE);

        // Persist values
        jcrPersist.persist("/test/mapped", bean, rr);

        // Check that everything stored correctly
        Resource res = rr.getResource("/test/mapped");
        ValueMap properties = res.getValueMap();
        // Part 1: Simple property
        assertEquals("Name", properties.get("prop-1", String.class));
        assertNull(properties.get("wrong1"));
        // Part 2: Array property
        String[] prop2 = properties.get("prop-2", String[].class);
        assertArrayEquals(prop2, new String[]{"foo", "bar", "baz"});
        assertNull(properties.get("wrong2"));
        // Part 3: Object property
        assertNull(rr.getResource("/test/mapped/wrong3"));
        Resource childRes1 = rr.getResource("/test/mapped/child-1");
        assertNotNull(childRes1);
        assertEquals("child-1", childRes1.getValueMap().get("name"));
        // Part 4: Object list property
        assertNull(rr.getResource("/test/mapped/wrong4"));
        Resource childRes2 = rr.getResource("/test/mapped/child-2");
        assertNotNull(childRes2);
        assertEquals(StreamSupport
                .stream(childRes2.getChildren().spliterator(), false)
                .count(), 3L);
        // Part 5: Map-of-objects property
        assertNull(rr.getResource("/test/mapped/wrong5"));
        Resource childRes3 = rr.getResource("/test/mapped/child-3");
        assertNotNull(childRes3);
        assertEquals(StreamSupport
                .stream(childRes3.getChildren().spliterator(), false)
                .count(), 3L);
        // Part 6: Boolean property
        assertNull(properties.get("wrong6"));
        assertNull(properties.get("isWrong6"));
        assertTrue(properties.get("prop-3", Boolean.class));

        // Now confirm Jackson respects its mappings too
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(bean);
        assertFalse("Should not have wrong property names: " + json, json.contains("wrong"));
        assertTrue("Should have prop-1" + json, json.contains("prop-1"));
        assertTrue("Should have prop-2" + json, json.contains("prop-2"));
        assertTrue("Should have prop-3" + json, json.contains("prop-3"));
        assertTrue("Should have child-1" + json, json.contains("child-1"));
        assertTrue("Should have child-2" + json, json.contains("child-2"));
        assertTrue("Should have child-3" + json, json.contains("child-3"));
    }

    @Test
    /**
     * Test named map children with map<String, Object>
     *
     */
    public void testMapChildrenWithStringKeys() throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        // Create some values in the Map<String, Object> data structure
        MappedChildren bean = new MappedChildren();
        MappedChildren.Child child1 = new MappedChildren.Child();
        bean.stringKeys.put("one", child1);
        MappedChildren.Child child2 = new MappedChildren.Child();
        bean.stringKeys.put("two", child2);
        MappedChildren.Child child3 = new MappedChildren.Child();
        bean.stringKeys.put("three", child3);
        child1.name = "one";
        child1.testValue = "Test Value 1";
        child2.name = "two";
        child2.testValue = "Test Value 2";
        child3.name = "three";
        child3.testValue = "Test Value 3";

        // Attempt to save the data structure
        jcrPersist.persist("/test/path", bean, rr);

        // Confirm the children were saved in the expected places using the map key as the node name
        Resource r1 = rr.getResource("/test/path/stringKeys/one");
        assertNotNull(r1);
        Resource r2 = rr.getResource("/test/path/stringKeys/two");
        assertNotNull(r2);
        Resource r3 = rr.getResource("/test/path/stringKeys/three");
        assertNotNull(r3);
    }

    @Test
    /**
     * Test named map children with map<String, Object>
     *
     */
    public void testMapChildrenWithEnumerationKeys() throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        // Do same thing except using enumKeys map on the bean object
        // e.g. --> bean.enumKeys.put(MappedChildren.KEYS.ONE, child1);

        // Create some values in the Map<String, Object> data structure
        MappedChildren bean = new MappedChildren();
        MappedChildren.Child child1 = new MappedChildren.Child();
        bean.enumKeys.put(MappedChildren.KEYS.ONE, child1);
        MappedChildren.Child child2 = new MappedChildren.Child();
        bean.enumKeys.put(MappedChildren.KEYS.TWO, child2);
        MappedChildren.Child child3 = new MappedChildren.Child();
        bean.enumKeys.put(MappedChildren.KEYS.THREE, child3);
        child1.name = "one";
        child1.testValue = "Test Value 1";
        child2.name = "two";
        child2.testValue = "Test Value 2";
        child3.name = "three";
        child3.testValue = "Test Value 3";

        // Attempt to save the data structure
        jcrPersist.persist("/test/path", bean, rr);

        // Confirm the children were saved in the expected places using the map key as the node name
        Resource r1 = rr.getResource("/test/path/enumKeys/ONE");
        assertNotNull(r1);
        Resource r2 = rr.getResource("/test/path/enumKeys/TWO");
        assertNotNull(r2);
        Resource r3 = rr.getResource("/test/path/enumKeys/THREE");
        assertNotNull(r3);
    }
}
