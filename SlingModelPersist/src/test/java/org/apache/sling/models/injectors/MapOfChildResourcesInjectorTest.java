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
 */package org.apache.sling.models.injectors;

import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.persist.ModelPersist;
import org.apache.sling.models.persist.impl.ModelPersistImpl;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Brendan Robert
 */
public class MapOfChildResourcesInjectorTest {
    @Rule
    public final SlingContext context = new SlingContext();

    ResourceResolver rr;
    ModelPersist jcrPersist;

    @Before
    public void setUp() {
        rr = context.resourceResolver();
        context.addModelsForPackage(this.getClass().getPackage().getName());
        context.registerService(Injector.class, new MapOfChildResourcesInjector());
        jcrPersist = new ModelPersistImpl();
    }

    @Test
    public void roundtripTest() throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        BeanWithMappedChildren source = new BeanWithMappedChildren();
        source.addPerson("joe", "schmoe");
        source.addPerson("john", "doe");
        source.addPerson("bob", "smith");
        jcrPersist.persist("/test/bean", source, rr);

        Resource targetResource = rr.getResource("/test/bean");
        assertNotNull("Bean should have been persisted");

        Resource personRes = rr.getResource("/test/bean/people/smith-bob");
        assertNotNull("Person should have persisted in repository", personRes);

        BeanWithMappedChildren target = targetResource.adaptTo(BeanWithMappedChildren.class);
        assertNotNull("Bean should deserialize", target);

        assertEquals("Should have 3 children", 3, target.people.size());
    }

    @Test
    public void roundtripEmpytTest() throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        BeanWithMappedChildren source = new BeanWithMappedChildren();
        jcrPersist.persist("/test/empty-bean", source, rr);

        Resource targetResource = rr.getResource("/test/empty-bean");
        assertNotNull("Bean should have been persisted");

        Resource personRes = rr.getResource("/test/empty-bean/people");
        assertNull("Person should not have persisted in repository", personRes);

        BeanWithMappedChildren target = targetResource.adaptTo(BeanWithMappedChildren.class);
        assertNotNull("Bean should deserialize", target);

        assertEquals("Should have 0 children", 0, target.people.size());
    }

    @Test
    public void roundtripTestDirectChildren() throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        BeanWithDirectMappedChildren source = new BeanWithDirectMappedChildren();
        source.addPerson("joe", "schmoe");
        source.addPerson("john", "doe");
        source.addPerson("bob", "smith");
        jcrPersist.persist("/test/bean", source, rr);

        Resource targetResource = rr.getResource("/test/bean");
        assertNotNull("Bean should have been persisted");

        Resource personRes = rr.getResource("/test/bean/smith-bob");
        assertNotNull("Person should have persisted in repository", personRes);

        BeanWithDirectMappedChildren target = targetResource.adaptTo(BeanWithDirectMappedChildren.class);
        assertNotNull("Bean should deserialize", target);

        assertEquals("Should have 3 children", 3, target.people.size());
    }
}
