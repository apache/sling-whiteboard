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

import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.thumbnails.internal.models.TransformationHandlerConfigImpl;
import org.apache.sling.thumbnails.BadRequestException;
import org.apache.sling.thumbnails.TransformationHandlerConfig;
import org.junit.Before;
import org.junit.Test;

public class CropHandlerTest {

    private ByteArrayOutputStream outputStream;
    private InputStream inputStream;
    private CropHandler cropper;

    @Before
    public void init() {
        inputStream = getClass().getClassLoader().getResourceAsStream("apache.png");
        outputStream = new ByteArrayOutputStream();
        cropper = new CropHandler();
    }

    @Test
    public void testCrop() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(CropHandler.PN_POSITION, "CENTER");
        properties.put(ResizeHandler.PN_HEIGHT, 200);
        properties.put(ResizeHandler.PN_WIDTH, 200);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        cropper.handle(inputStream, outputStream, config);
        assertNotEquals(0, outputStream.toByteArray().length);
    }

    @Test
    public void testCropLower() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(CropHandler.PN_POSITION, "center");
        properties.put(ResizeHandler.PN_HEIGHT, 200);
        properties.put(ResizeHandler.PN_WIDTH, 200);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        cropper.handle(inputStream, outputStream, config);
        assertNotEquals(0, outputStream.toByteArray().length);
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidPosition() throws IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(CropHandler.PN_POSITION, "centerz");
        properties.put(ResizeHandler.PN_HEIGHT, 200);
        properties.put(ResizeHandler.PN_WIDTH, 200);
        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        cropper.handle(inputStream, outputStream, config);
    }

    @Test(expected = BadRequestException.class)
    public void testMissingWidthHeight() throws IOException {
        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf",
                Collections.singletonMap(CropHandler.PN_POSITION, "center"));
        cropper.handle(inputStream, outputStream, config);
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidHuge() throws IOException {

        Map<String, Object> properties = new HashMap<>();
        properties.put(ResizeHandler.PN_WIDTH, Integer.MAX_VALUE);
        properties.put(ResizeHandler.PN_HEIGHT, Integer.MAX_VALUE);

        TransformationHandlerConfig config = new TransformationHandlerConfigImpl("/conf", properties);
        cropper.handle(inputStream, outputStream, config);
    }

}
