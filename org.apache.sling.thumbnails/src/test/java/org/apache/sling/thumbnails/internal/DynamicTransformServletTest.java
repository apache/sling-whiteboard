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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.apache.sling.thumbnails.Transformation;
import org.apache.sling.thumbnails.TransformationHandlerConfig;
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
        when(thumbnailSupport.getPersistableTypes()).thenReturn(Collections.singleton("sling:File"));
        when(thumbnailSupport.getRenditionPath("sling:File")).thenReturn("jcr:content/renditions");
        Set<String> supportedTypes = new HashSet<>();
        supportedTypes.add("nt:file");
        supportedTypes.add("sling:File");
        when(thumbnailSupport.getSupportedTypes()).thenReturn(supportedTypes);
        when(thumbnailSupport.getMetaTypePropertyPath(anyString())).thenReturn("jcr:content/jcr:mimeType");

        TransformationServiceUser tsu = mock(TransformationServiceUser.class);
        when(tsu.getTransformationServiceUser()).thenReturn(context.resourceResolver());

        RenditionSupportImpl renditionSupport = new RenditionSupportImpl(thumbnailSupport, tsu);
        TransformerImpl transformer = new TransformerImpl(providers, thumbnailSupport, th);
        dts = new DynamicTransformServlet(transformer, renditionSupport);

    }

    @Test
    public void testRequest() throws IOException, ServletException {

        context.request().addRequestParameter("resource", "/content/apache/sling-apache-org/index/apache.png");
        context.request().addRequestParameter("format", "png");
        context.request().setContent(
                "[{\"handlerType\":\"sling/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
                        .getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("image/png", context.response().getContentType());

        assertNotEquals(0, context.response().getOutput().length);
    }

    @Test
    public void testInvalidPersist() throws IOException, ServletException {

        context.request().addRequestParameter("resource", "/content/apache/sling-apache-org/index/apache.png");
        context.request().addRequestParameter("renditionName", "/my-rendition.png");
        context.request().addRequestParameter("format", "png");
        context.request().setContent(
                "[{\"handlerType\":\"sling/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
                        .getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
    }

    @Test
    public void testPersist() throws IOException, ServletException {

        context.create().resource("/content/slingfile.jpg",
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, "sling:File"));
        Map<String, Object> slingFileProperties = new HashMap<>();
        slingFileProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        slingFileProperties.put(JcrConstants.JCR_DATA, context.resourceResolver()
                .getResource("/content/apache/sling-apache-org/index/apache.png").adaptTo(InputStream.class));
        slingFileProperties.put("jcr:mimeType", "image/jpeg");
        context.create().resource("/content/slingfile.jpg/jcr:content", slingFileProperties);

        context.request().addRequestParameter("resource", "/content/slingfile.jpg");
        context.request().addRequestParameter("renditionName", "my-rendition.png");
        context.request().addRequestParameter("format", "png");
        context.request().setContent(
                "[{\"handlerType\":\"sling/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
                        .getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(200, context.response().getStatus());

        assertNotNull(context.resourceResolver()
                .getResource("/content/slingfile.jpg/jcr:content/renditions/my-rendition.png"));
    }

    @Test
    public void testNoResource() throws IOException, ServletException {

        context.request().addRequestParameter("format", "png");
        context.request().setContent(
                "[{\"handlerType\":\"sling/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
                        .getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
    }

    @Test
    public void testMissingResource() throws IOException, ServletException {

        context.request().addRequestParameter("resource",
                "/content/apache/sling-apache-org/index/wow-look-at-this-file.png");
        context.request().addRequestParameter("format", "png");
        context.request().setContent(
                "[{\"handlerType\":\"sling/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
                        .getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(404, context.response().getStatus());
    }

    @Test
    public void testRequestWithResource() throws IOException, ServletException {

        context.create().resource("/home/users/test/transformation");

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

        Transformation transformation = new TransformationImpl(handlers);

        context.registerAdapter(Resource.class, Transformation.class, transformation);

        context.request().addRequestParameter("resource", "/content/apache/sling-apache-org/index/apache.png");
        context.request().addRequestParameter("format", "png");
        context.request().addRequestParameter("transformationResource", "/home/users/test/transformation");
        context.request().setContent(
                "[{\"handlerType\":\"sling/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
                        .getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("image/png", context.response().getContentType());

        assertNotEquals(0, context.response().getOutput().length);
    }

    @Test
    public void testRequestWithInvalidResource() throws IOException, ServletException {

        context.request().addRequestParameter("resource", "/content/apache/sling-apache-org/index/apache.png");
        context.request().addRequestParameter("format", "png");
        context.request().addRequestParameter("transformationResource", "/home/users/test/transformation");
        context.request().setContent(
                "[{\"handlerType\":\"sling/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
                        .getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
    }

    @Test
    public void testRequestWithFailedAdaption() throws IOException, ServletException {

        context.create().resource("/home/users/test/transformation");
        context.request().addRequestParameter("resource", "/content/apache/sling-apache-org/index/apache.png");
        context.request().addRequestParameter("format", "png");
        context.request().addRequestParameter("transformationResource", "/home/users/test/transformation");
        context.request().setContent(
                "[{\"handlerType\":\"sling/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
                        .getBytes());
        dts.doPost(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
    }

    @Test
    public void testNoFormat() throws IOException, ServletException {

        context.request().addRequestParameter("resource", "/content/apache/sling-apache-org/index/apache.png");
        context.request().setContent(
                "[{\"handlerType\":\"sling/thumbnails/transformers/crop\",\"properties\":{\"position\":\"CENTER\",\"width\":1000,\"height\":1000}}]"
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
