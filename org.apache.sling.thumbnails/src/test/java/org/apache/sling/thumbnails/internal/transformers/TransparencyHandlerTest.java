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
package org.apache.sling.thumbnails.internal.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.thumbnails.internal.models.TransformationHandlerConfigImpl;
import org.apache.sling.thumbnails.BadRequestException;
import org.apache.sling.thumbnails.TransformationHandlerConfig;
import org.junit.Before;
import org.junit.Test;

public class TransparencyHandlerTest {

    private ByteArrayOutputStream outputStream;
    private InputStream inputStream;
    private TransparencyHandler transparent;

    @Before
    public void init() {
        inputStream = getClass().getClassLoader().getResourceAsStream("apache.png");
        outputStream = new ByteArrayOutputStream();
        transparent = new TransparencyHandler();
    }

    @Test
    public void testResourceType() throws IOException {
        assertEquals("sling/thumbnails/transformers/transparency", transparent.getResourceType());
    }

    @Test
    public void testTransparency() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ColorizeHandler.PN_ALPHA, .8);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        transparent.handle(inputStream, outputStream, config);
        assertNotEquals(0, outputStream.toByteArray().length);
    }

    @Test
    public void testNoAlpha() throws IOException {
        Map<String, Object> properties = new HashMap<>();

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        transparent.handle(inputStream, outputStream, config);
        assertNotEquals(0, outputStream.toByteArray().length);
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidAlpha() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ColorizeHandler.PN_ALPHA, -.8);
        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        transparent.handle(inputStream, outputStream, config);
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidLargeAlpha() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ColorizeHandler.PN_ALPHA, 2.0);
        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        transparent.handle(inputStream, outputStream, config);
    }

}
