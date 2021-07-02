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
package org.apache.sling.commons.thumbnails.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.commons.thumbnails.ThumbnailProvider;
import org.apache.sling.commons.thumbnails.ThumbnailSupport;
import org.apache.sling.commons.thumbnails.TransformationHandler;
import org.apache.sling.commons.thumbnails.internal.providers.ImageThumbnailProvider;
import org.apache.sling.commons.thumbnails.internal.providers.PdfThumbnailProvider;
import org.apache.sling.commons.thumbnails.internal.transformers.CropHandler;
import org.apache.sling.commons.thumbnails.internal.transformers.ResizeHandler;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DynamicTransformServletTest {

    private DynamicTransformServlet dts;

    @Rule
    public final SlingContext context = new SlingContext();

    @Before
    public void init() throws IllegalAccessException, LoginException {

        ContextHelper.initContext(context);

        List<TransformationHandler> th = new ArrayList<>();
        th.add(new CropHandler());
        th.add(new ResizeHandler());

        List<ThumbnailProvider> providers = new ArrayList<>();
        providers.add(new ImageThumbnailProvider());
        providers.add(new PdfThumbnailProvider());

        ThumbnailSupport thumbnailSupport = mock(ThumbnailSupport.class);
        when(thumbnailSupport.getPersistableTypes()).thenReturn(Collections.emptySet());
        when(thumbnailSupport.getSupportedTypes()).thenReturn(Collections.singleton("nt:file"));
        when(thumbnailSupport.getMetaTypePropertyPath("nt:file")).thenReturn("jcr:content/jcr:mimeType");

        TransformerImpl transformer = new TransformerImpl(providers, thumbnailSupport, th);
        dts = new DynamicTransformServlet(transformer);

    }

    @Test
    public void testRequest() throws IOException, ServletException {

        context.request().addRequestParameter("resource", "/content/apache/sling-apache-org/index/apache.png");
        context.request().addRequestParameter("format", "png");
        context.request().setContent(
                "[{\"handlerType\":\"sling/commons/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
                        .getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("image/png", context.response().getContentType());

        assertNotEquals(0, context.response().getOutput().length);
    }

    @Test
    public void testNoFormat() throws IOException, ServletException {

        context.request().addRequestParameter("resource", "/content/apache/sling-apache-org/index/apache.png");
        context.request().setContent(
                "[{\"handlerType\":\"sling/commons/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
                        .getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("image/jpeg", context.response().getContentType());

        assertNotEquals(0, context.response().getOutput().length);
    }

    @Test
    public void testNoHandlers() throws IOException, ServletException {

        context.request().addRequestParameter("resource", "/content/apache/sling-apache-org/index/apache.png");
        context.request().setContent("[]".getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("image/jpeg", context.response().getContentType());

        assertNotEquals(0, context.response().getOutput().length);
    }

    @Test
    public void testInvalidJson() throws IOException, ServletException {

        context.request().addRequestParameter("resource", "/content/apache/sling-apache-org/index/apache.png");
        context.request().setContent("{}".getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
    }

}
