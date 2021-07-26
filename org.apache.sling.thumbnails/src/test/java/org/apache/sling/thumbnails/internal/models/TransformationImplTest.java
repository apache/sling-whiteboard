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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import javax.jcr.LoginException;

import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.thumbnails.Transformation;
import org.apache.sling.thumbnails.TransformationHandlerConfig;
import org.apache.sling.thumbnails.internal.ContextHelper;
import org.apache.sling.thumbnails.internal.transformers.RotateHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TransformationImplTest {

    @Rule
    public final SlingContext context = new SlingContext();

    @Before
    public void init() throws IllegalAccessException, LoginException {
        ContextHelper.initContext(context);
        context.addModelsForPackage("org.apache.sling.thumbnails.internal.models");
    }

    @Test
    public void testModel() {
        Transformation transformation = context.resourceResolver()
                .getResource("/conf/global/files/transformations/sling-cms-thumbnail").adaptTo(Transformation.class);
        assertNotNull(transformation);
        assertEquals("sling-cms-thumbnail", transformation.getName());
        assertEquals("/conf/global/files/transformations/sling-cms-thumbnail", transformation.getPath());

        assertEquals(1, transformation.getHandlers().size());

        assertEquals("sling/thumbnails/transformers/crop", transformation.getHandlers().get(0).getHandlerType());
    }

    @Test
    public void testJson() {

        List<TransformationHandlerConfig> config = Collections.singletonList(new TransformationHandlerConfigImpl(
                "sling/thumbnails/transformers/rotate", Collections.singletonMap(RotateHandler.DEGREES, 90)));

        Transformation transformation = new TransformationImpl(config);

        assertNotNull(transformation);
        assertNull(transformation.getName());
        assertNull(transformation.getPath());

        assertEquals(1, transformation.getHandlers().size());
        assertTrue(transformation.getHandlers().get(0) instanceof TransformationHandlerConfig);
        assertEquals("sling/thumbnails/transformers/rotate", transformation.getHandlers().get(0).getHandlerType());

    }

}
