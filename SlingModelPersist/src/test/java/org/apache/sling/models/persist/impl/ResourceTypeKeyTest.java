/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.sling.models.persist.impl;

import org.apache.sling.models.persist.bean.BeanWithPathField;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test various behaviors of ResourceTypeKey class
 */
public class ResourceTypeKeyTest {

    public ResourceTypeKeyTest() {
    }

    @Before
    public void setUp() {
    }

    /**
     * Test of fromObject method, of class ResourceTypeKey relying on class annotations
     */
    @Test
    public void testFromObject() {
        BeanWithPathField bean = new BeanWithPathField();
        ResourceTypeKey result = ResourceTypeKey.fromObject(bean);
        assertEquals("test/testBean", result.primaryType);
        assertEquals("test/testBean/field-path", result.childType);
    }

    @Test
    public void testFromObjectCache() {
        BeanWithPathField bean1 = new BeanWithPathField();
        BeanWithPathField bean2 = new BeanWithPathField();
        ResourceTypeKey result1 = ResourceTypeKey.fromObject(bean1);
        ResourceTypeKey result2 = ResourceTypeKey.fromObject(bean2);
        assertEquals("Should use the same object value", result1, result2);
    }

    @Test
    public void testNullObject() {
        ResourceTypeKey result = ResourceTypeKey.fromObject(null);
        assertNotNull("Should not return null", result);
        assertEquals("Should be NT UNSTRUCTURED", "nt:unstructured", result.primaryType);
    }
}
