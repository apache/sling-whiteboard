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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.thumbnails.ThumbnailProvider;
import org.apache.sling.commons.thumbnails.ThumbnailSupport;
import org.apache.sling.commons.thumbnails.TransformationHandler;
import org.apache.sling.commons.thumbnails.TransformationHandlerConfig;
import org.apache.sling.commons.thumbnails.internal.models.TransformationHandlerConfigImpl;
import org.apache.sling.commons.thumbnails.internal.models.TransformationImpl;
import org.apache.sling.commons.thumbnails.internal.providers.ImageThumbnailProvider;
import org.apache.sling.commons.thumbnails.internal.providers.PdfThumbnailProvider;
import org.apache.sling.commons.thumbnails.internal.transformers.CropHandler;
import org.apache.sling.commons.thumbnails.internal.transformers.ResizeHandler;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformServletTest {

    private static final Logger log = LoggerFactory.getLogger(TransformServletTest.class);

    private TransformServlet ts;

    @Rule
    public final SlingContext context = new SlingContext();

    @Before
    public void init() throws IllegalAccessException, LoginException {
        ContextHelper.initContext(context);

        ResourceResolverFactory factory = Mockito.mock(ResourceResolverFactory.class);
        ResourceResolver resolver = Mockito.mock(ResourceResolver.class);

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

        TransformationImpl transformation = new TransformationImpl(handlers, "test");

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getPath()).thenReturn("/conf");
        Mockito.when(resource.adaptTo(Mockito.any())).thenReturn(transformation);
        Mockito.when(resolver.findResources(Mockito.anyString(), Mockito.anyString())).thenAnswer((ans) -> {
            List<Resource> resources = new ArrayList<>();
            if (ans.getArgument(0, String.class).contains("test")) {
                resources.add(resource);
            }
            return resources.iterator();
        });
        when(resolver.getResource("/conf")).thenReturn(resource);

        Mockito.when(factory.getServiceResourceResolver(Mockito.any())).thenReturn(resolver);
        TransformationServiceUser tsu = new TransformationServiceUser(factory);

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
        ts = new TransformServlet(thumbnailSupport, transformer, tsu, new TransformationCache(tsu),
                mock(BundleContext.class));

    }

    @Test
    public void testValid() throws IOException, ServletException {
        log.info("testContentTypes");

        context.currentResource("/content/apache/sling-apache-org/index/apache.png");
        context.requestPathInfo().setSuffix("/test.png");
        context.requestPathInfo().setExtension("transform");

        ts.doGet(context.request(), context.response());

        assertNotNull(context.response().getOutput());
    }

    @Test
    public void testInvalid() throws IOException, ServletException {
        log.info("testContentTypes");

        context.currentResource("/content/apache/sling-apache-org/index/apache.png");
        context.requestPathInfo().setSuffix("/te.png");
        context.requestPathInfo().setExtension("transform");

        ts.doGet(context.request(), context.response());

        assertEquals(404, context.response().getStatus());
    }

}
