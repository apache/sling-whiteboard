/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.thumbnails.internal.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.thumbnails.Transformation;
import org.junit.Before;
import org.junit.Test;

public class TransformationManagerImplTest {

    private TransformationManagerImpl manager;

    private Resource resource;
    private ConfigurationResourceResolver configurationResourceResolver;
    private Transformation transformation;

    @Before
    public void init() {
        resource = mock(Resource.class);
        configurationResourceResolver = mock(ConfigurationResourceResolver.class);
        manager = new TransformationManagerImpl(resource, configurationResourceResolver);

        Resource configResource = mock(Resource.class);

        transformation = mock(Transformation.class);
        when(configResource.adaptTo(Transformation.class)).thenReturn(transformation);

        when(configurationResourceResolver.getResourceCollection(resource, "files", "transformations"))
                .thenReturn(Collections.singleton(configResource));
    }

    @Test
    public void testManager(){
        Collection<Transformation> transformations = manager.getTransformations();
        assertNotNull(transformations);
        assertEquals(1, transformations.size());
        assertEquals(transformation, transformations.iterator().next());
    }

}
