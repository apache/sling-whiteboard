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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.pdfbox.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.servlethelpers.MockRequestDispatcherFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.thumbnails.ThumbnailSupport;
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
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

public class TransformServletTest {

    private TransformServlet ts;

    @Rule
    public final SlingContext context = new SlingContext();

    private Resource resource;

    private RequestDispatcher dispatcher;

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

        TransformationImpl transformation = new TransformationImpl(handlers);

        resource = Mockito.mock(Resource.class);
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
        when(thumbnailSupport.getPersistableTypes()).thenReturn(Collections.singleton("sling:File"));
        when(thumbnailSupport.getRenditionPath("sling:File")).thenReturn("jcr:content/renditions");
        Set<String> supportedTypes = new HashSet<>();
        supportedTypes.add("sling:File");
        supportedTypes.add("nt:file");
        when(thumbnailSupport.getSupportedTypes()).thenReturn(supportedTypes);
        when(thumbnailSupport.getMetaTypePropertyPath(anyString())).thenReturn("jcr:content/jcr:mimeType");
        when(thumbnailSupport.getServletErrorResourcePath()).thenReturn("/content");

        TransformerImpl transformer = new TransformerImpl(providers, thumbnailSupport, th);

        ResourceResolverFactory contextFactory = Mockito.mock(ResourceResolverFactory.class);
        Mockito.when(contextFactory.getServiceResourceResolver(Mockito.any())).thenReturn(context.resourceResolver());
        TransformationServiceUser contextTsu = new TransformationServiceUser(contextFactory);
        RenditionSupportImpl renditionSupport = new RenditionSupportImpl(thumbnailSupport, contextTsu);

        ts = new TransformServlet(thumbnailSupport, transformer, tsu, new TransformationCache(tsu), renditionSupport,
                mock(BundleContext.class));

        MockRequestDispatcherFactory dispatcherFactory = mock(MockRequestDispatcherFactory.class);
        dispatcher = mock(RequestDispatcher.class);
        when(dispatcherFactory.getRequestDispatcher(anyString(), any())).thenReturn(dispatcher);
        context.request().setRequestDispatcherFactory(dispatcherFactory);

    }

    @Test
    public void testValid() throws IOException, ServletException {
        context.currentResource("/content/apache/sling-apache-org/index/apache.png");
        context.requestPathInfo().setSuffix("/test.png");
        context.requestPathInfo().setExtension("transform");

        ts.doGet(context.request(), context.response());

        assertNotNull(context.response().getOutput());
    }

    @Test
    public void testPersistence() throws IOException, ServletException {

        context.create().resource("/content/slingfile.jpg",
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, "sling:File"));
        Map<String, Object> slingFileProperties = new HashMap<>();
        slingFileProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        slingFileProperties.put(JcrConstants.JCR_DATA,
                IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("apache.png")));
        slingFileProperties.put("jcr:mimeType", "image/jpeg");
        context.create().resource("/content/slingfile.jpg/jcr:content", slingFileProperties);

        context.currentResource("/content/slingfile.jpg");
        context.requestPathInfo().setSuffix("/test.png");
        context.requestPathInfo().setExtension("transform");

        ts.doGet(context.request(), context.response());

        assertNotNull(context.response().getOutput());

        assertNotNull(context.resourceResolver().getResource("/content/slingfile.jpg/jcr:content/renditions/test.png"));
    }

    @Test
    public void testUnsupportedOutput() throws IOException, ServletException {

        context.currentResource("/content/apache/sling-apache-org/index/apache.png");
        context.requestPathInfo().setSuffix("/test.webp");
        context.requestPathInfo().setExtension("transform");

        ts.doGet(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
    }

    @Test
    public void testUnexpectedException() throws IOException, ServletException {

        Resource throwy = mock(Resource.class);
        when(throwy.getResourceType()).thenThrow(new RuntimeException());
        context.currentResource(throwy);
        context.requestPathInfo().setSuffix("/test.jpg");
        context.requestPathInfo().setExtension("transform");

        ts.doGet(context.request(), context.response());

        assertEquals(500, context.response().getStatus());

        verify(dispatcher).forward(any(), any());
    }

    @Test
    public void testInvalidConfig() throws ServletException, IOException {
        List<TransformationHandlerConfig> handlers = new ArrayList<>();
        Map<String, Object> crop = new HashMap<>();
        crop.put(CropHandler.PN_POSITION, "center");
        crop.put(ResizeHandler.PN_WIDTH, -1);
        crop.put(ResizeHandler.PN_HEIGHT, 200);
        handlers.add(new TransformationHandlerConfigImpl(CropHandler.RESOURCE_TYPE, crop));

        TransformationImpl transformation = new TransformationImpl(handlers);

        Mockito.when(resource.adaptTo(Mockito.any())).thenReturn(transformation);

        context.currentResource("/content/apache/sling-apache-org/index/apache.png");
        context.requestPathInfo().setSuffix("/test.png");
        context.requestPathInfo().setExtension("transform");

        ts.doGet(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
    }

    @Test
    public void testInvalid() throws IOException, ServletException {

        context.currentResource("/content/apache/sling-apache-org/index/apache.png");
        context.requestPathInfo().setSuffix("/te.png");
        context.requestPathInfo().setExtension("transform");

        ts.doGet(context.request(), context.response());

        assertEquals(404, context.response().getStatus());
    }

}
