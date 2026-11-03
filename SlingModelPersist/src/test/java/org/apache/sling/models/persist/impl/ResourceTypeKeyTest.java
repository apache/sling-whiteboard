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
package org.apache.sling.models.persist.impl;

import org.apache.sling.models.persistor.impl.ResourceTypeKey;
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
    public void testFromObject() throws IllegalAccessException {
        BeanWithPathField bean = new BeanWithPathField();
        ResourceTypeKey result = ResourceTypeKey.fromObject(bean);
        assertEquals("Should be NT UNSTRUCTURED", "nt:unstructured", result.nodeType);
        assertEquals("test/testBean", result.resourceType);
        // Child type wasn't making much sense, this was removed in favor of objects creating their own jcr:content children
        // It was a tough call but ultimately makes it easier to understand what's going on since there are fewer rules to remember.
        //assertEquals("test/testBean/field-path", result.childType);
    }

    @Test
    public void testFromObjectCache() throws IllegalAccessException {
        BeanWithPathField bean1 = new BeanWithPathField();
        BeanWithPathField bean2 = new BeanWithPathField();
        ResourceTypeKey result1 = ResourceTypeKey.fromObject(bean1);
        ResourceTypeKey result2 = ResourceTypeKey.fromObject(bean2);
        assertEquals("Should use the same object value", result1, result2);
    }

    @Test
    public void testNullObject() throws IllegalAccessException {
        ResourceTypeKey result = ResourceTypeKey.fromObject(null);
        assertNotNull("Should not return null", result);
        assertEquals("Should be NT UNSTRUCTURED", "nt:unstructured", result.nodeType);
    }
}
