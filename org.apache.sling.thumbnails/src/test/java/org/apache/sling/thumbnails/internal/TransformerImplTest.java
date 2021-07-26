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
package org.apache.sling.thumbnails.internal;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.thumbnails.BadRequestException;
import org.apache.sling.thumbnails.OutputFileFormat;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.apache.sling.thumbnails.TransformationHandlerConfig;
import org.apache.sling.thumbnails.Transformer;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.apache.sling.thumbnails.extension.TransformationHandler;
import org.apache.sling.thumbnails.internal.models.TransformationHandlerConfigImpl;
import org.apache.sling.thumbnails.internal.models.TransformationImpl;
import org.apache.sling.thumbnails.internal.providers.ImageThumbnailProvider;
import org.apache.sling.thumbnails.internal.providers.PdfThumbnailProvider;
import org.apache.sling.thumbnails.internal.transformers.CropHandler;
import org.apache.sling.thumbnails.internal.transformers.ResizeHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TransformerImplTest {
    @Rule
    public final SlingContext context = new SlingContext();
    private Transformer transformer;

    @Before
    public void init() {
        ContextHelper.initContext(context);

        List<TransformationHandler> handlers = new ArrayList<>();
        handlers.add(new CropHandler());
        handlers.add(new ResizeHandler());

        List<ThumbnailProvider> providers = new ArrayList<>();
        providers.add(new ImageThumbnailProvider());
        providers.add(new PdfThumbnailProvider());

        ThumbnailSupport thumbnailSupport = mock(ThumbnailSupport.class);
        when(thumbnailSupport.getPersistableTypes()).thenReturn(Collections.emptySet());
        when(thumbnailSupport.getSupportedTypes()).thenReturn(Collections.singleton("nt:file"));
        when(thumbnailSupport.getMetaTypePropertyPath("nt:file")).thenReturn("jcr:content/jcr:mimeType");
        when(thumbnailSupport.getServletErrorSuffix()).thenReturn("error");
        when(thumbnailSupport.getServletErrorResourcePath()).thenReturn("/content/sling/error");

        transformer = new TransformerImpl(providers, thumbnailSupport, handlers);

    }

    @Test
    public void testImageThumbnail() throws IOException {
        context.currentResource("/content/apache/sling-apache-org/index/apache.png");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<TransformationHandlerConfig> handlers = new ArrayList<>();

        Map<String, Object> size = new HashMap<>();
        size.put(ResizeHandler.PN_WIDTH, 200);
        size.put(ResizeHandler.PN_HEIGHT, 200);
        handlers.add(new TransformationHandlerConfigImpl(ResizeHandler.RESOURCE_TYPE, size));

        Map<String, Object> crop = new HashMap<>();
        crop.put(CropHandler.PN_POSITION, "center");
        crop.put(ResizeHandler.PN_WIDTH, 200);
        crop.put(ResizeHandler.PN_HEIGHT, 200);
        handlers.add(new TransformationHandlerConfigImpl(CropHandler.RESOURCE_TYPE, crop));

        TransformationImpl transformation = new TransformationImpl(handlers, "test", mock(Resource.class));
        transformer.transform(context.currentResource(), transformation, OutputFileFormat.PNG, baos);
        assertNotNull(baos);
    }

    @Test(expected = BadRequestException.class)
    public void testNotFile() throws IOException {
        context.currentResource("/content/apache/sling-apache-org/index");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<TransformationHandlerConfigImpl> handlers = new ArrayList<>();

        Map<String, Object> size = new HashMap<>();
        size.put(ResizeHandler.PN_WIDTH, 200);
        size.put(ResizeHandler.PN_HEIGHT, 200);
        handlers.add(new TransformationHandlerConfigImpl(ResizeHandler.RESOURCE_TYPE, size));

        Map<String, Object> crop = new HashMap<>();
        crop.put(CropHandler.PN_POSITION, "center");
        crop.put(ResizeHandler.PN_WIDTH, 200);
        crop.put(ResizeHandler.PN_HEIGHT, 200);
        handlers.add(new TransformationHandlerConfigImpl(CropHandler.RESOURCE_TYPE, crop));

        TransformationImpl transformation = new TransformationImpl(handlers);
        transformer.transform(context.currentResource(), transformation, OutputFileFormat.PNG, baos);
    }

}
