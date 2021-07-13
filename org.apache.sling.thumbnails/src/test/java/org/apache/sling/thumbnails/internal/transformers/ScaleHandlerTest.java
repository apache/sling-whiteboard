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

public class ScaleHandlerTest {

    private InputStream inputStream;
    private ByteArrayOutputStream outputStream;
    private ScaleHandler scaler;

    @Before
    public void init() {
        inputStream = getClass().getClassLoader().getResourceAsStream("apache.png");
        outputStream = new ByteArrayOutputStream();
        scaler = new ScaleHandler();
    }

    @Test
    public void testResourceType() throws IOException {
        assertEquals("sling/thumbnails/transformers/scale", scaler.getResourceType());
    }

    @Test
    public void testScaleBoth() throws IOException {

        Map<String, Object> properties = new HashMap<>();
        properties.put(ScaleHandler.PN_BOTH, 2);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        scaler.handle(inputStream, outputStream, config);
        assertNotEquals(0, outputStream.toByteArray().length);
    }

    @Test
    public void testScaleWidthAndHeight() throws IOException {

        Map<String, Object> properties = new HashMap<>();
        properties.put(ResizeHandler.PN_WIDTH, 2);
        properties.put(ResizeHandler.PN_HEIGHT, 1);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        scaler.handle(inputStream, outputStream, config);
        assertNotEquals(0, outputStream.toByteArray().length);
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidScale() throws IOException {

        Map<String, Object> properties = new HashMap<>();
        properties.put(ResizeHandler.PN_WIDTH, 2);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        scaler.handle(inputStream, outputStream, config);
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidScaleWidth() throws IOException {

        Map<String, Object> properties = new HashMap<>();
        properties.put(ResizeHandler.PN_HEIGHT, 2);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        scaler.handle(inputStream, outputStream, config);
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidHugeScale() throws IOException {

        Map<String, Object> properties = new HashMap<>();
        properties.put(ScaleHandler.PN_BOTH, Integer.MAX_VALUE);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        scaler.handle(inputStream, outputStream, config);
    }
}
