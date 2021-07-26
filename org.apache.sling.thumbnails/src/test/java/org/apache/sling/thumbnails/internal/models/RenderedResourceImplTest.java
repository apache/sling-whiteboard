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
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.thumbnails.RenderedResource;
import org.apache.sling.thumbnails.RenditionSupport;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.apache.sling.thumbnails.Transformation;
import org.apache.sling.thumbnails.internal.ContextHelper;
import org.apache.sling.thumbnails.internal.RenditionSupportImpl;
import org.apache.sling.thumbnails.internal.TransformationServiceUser;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RenderedResourceImplTest {

    @Rule
    public final SlingContext context = new SlingContext();

    private @NotNull Resource ntFileresource;

    private @NotNull Resource slingFileResource;

    @Before
    public void init() throws IllegalAccessException, LoginException {

        ContextHelper.initContext(context);

        ThumbnailSupport thumbnailSupport = mock(ThumbnailSupport.class);
        when(thumbnailSupport.getPersistableTypes()).thenReturn(Collections.singleton("sling:File"));
        when(thumbnailSupport.getRenditionPath("sling:File")).thenReturn("jcr:content/renditions");
        when(thumbnailSupport.getRenditionPath(not(eq("sling:File"))))
                .thenThrow(new IllegalArgumentException("Supplied non-persistable resource type!"));

        Set<String> supportedTypes = new HashSet<>();
        supportedTypes.add("sling:File");
        supportedTypes.add("nt:file");
        when(thumbnailSupport.getSupportedTypes()).thenReturn(supportedTypes);
        when(thumbnailSupport.getMetaTypePropertyPath(anyString())).thenReturn("jcr:content/jcr:mimeType");

        context.registerService(ThumbnailSupport.class, thumbnailSupport);

        TransformationServiceUser tsu = mock(TransformationServiceUser.class);
        when(tsu.getTransformationServiceUser()).thenReturn(context.resourceResolver());

        RenditionSupport renditionSupport = new RenditionSupportImpl(thumbnailSupport, tsu);
        context.registerService(RenditionSupport.class, renditionSupport);

        ConfigurationResourceResolver configurationResourceResolver = mock(ConfigurationResourceResolver.class);
        Resource configResource = mock(Resource.class);
        Transformation transformation = mock(Transformation.class);
        when(transformation.getName()).thenReturn("test");
        when(configResource.adaptTo(Transformation.class)).thenReturn(transformation);
        when(configurationResourceResolver.getResourceCollection(any(), eq("files"), eq("transformations")))
                .thenReturn(Collections.singleton(configResource));
        context.registerService(ConfigurationResourceResolver.class, configurationResourceResolver);

        Map<String, Object> ntFileProperties = new HashMap<>();
        ntFileProperties.put("jcr:primaryType", JcrConstants.NT_FILE);
        ntFileProperties.put("jcr:content/jcr:primaryType", JcrConstants.NT_RESOURCE);
        ntFileProperties.put("jcr:content/jcr:data", new byte[] { 1, 0 });
        ntFileProperties.put("jcr:content/jcr:mimeType", "image/jpeg");
        ntFileresource = context.create().resource("/content/ntfile.jpg", ntFileProperties);

        slingFileResource = context.create().resource("/content/slingfile.jpg",
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, "sling:File"));
        Map<String, Object> slingFileProperties = new HashMap<>();
        slingFileProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        slingFileProperties.put(JcrConstants.JCR_DATA, new byte[] { 1, 0 });
        slingFileProperties.put("jcr:mimeType", "image/jpeg");
        context.create().resource("/content/slingfile.jpg/jcr:content", slingFileProperties);
        context.create().resource("/content/slingfile.jpg/jcr:content/renditions",
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, "sling:Folder"));

        context.addModelsForClasses(RenderedResourceImpl.class);

        ((MockRequestPathInfo) context.request().getRequestPathInfo()).setSuffix(slingFileResource.getPath());

    }

    @Test
    public void testGetRenditions() {
        RenderedResource rendered = context.request().adaptTo(RenderedResource.class);
        assertNotNull(rendered);
        assertNotNull(rendered.getRenditions());
        assertEquals(0, rendered.getRenditions().size());
        assertEquals("jcr:content/renditions", rendered.getRenditionsPath());

    }

    private void addRendition(String filePath, String renditionName) {

        Map<String, Object> renditionProperties = new HashMap<>();
        renditionProperties.put("jcr:primaryType", JcrConstants.NT_FILE);
        renditionProperties.put("jcr:content/jcr:primaryType", JcrConstants.NT_RESOURCE);
        renditionProperties.put("jcr:content/jcr:data", new byte[] { 1, 0 });
        renditionProperties.put("jcr:content/jcr:mimeType", "image/png");
        context.create().resource(filePath + "/jcr:content/renditions/" + renditionName, renditionProperties);
    }

    @Test
    public void testRenditionExists() {
        addRendition("/content/slingfile.jpg", "test.png");
        addRendition("/content/slingfile.jpg", "test2.png");

        RenderedResource rendered = context.request().adaptTo(RenderedResource.class);
        assertEquals(2, rendered.getRenditions().size());
    }

    @Test
    public void testInvalidResource() throws PersistenceException {
        ((MockRequestPathInfo) context.request().getRequestPathInfo()).setSuffix(ntFileresource.getPath());
        RenderedResource rendered = context.request().adaptTo(RenderedResource.class);
        assertNotNull(rendered);
        assertNull(rendered.getRenditionsPath());
        assertEquals(0, rendered.getRenditions().size());
    }

    @Test
    public void testSupportedRenditions() {
        RenderedResource rendered = context.request().adaptTo(RenderedResource.class);
        List<String> supportedRenditions = rendered.getSupportedRenditions();
        assertNotNull(supportedRenditions);
        assertEquals(1, supportedRenditions.size());
        assertEquals("test", supportedRenditions.get(0));
    }

    @Test
    public void testSupportedRenditionsMerge() {
        addRendition("/content/slingfile.jpg", "test.png");
        addRendition("/content/slingfile.jpg", "test2.png");
        addRendition("/content/slingfile.jpg", "test2.jpeg");
        RenderedResource rendered = context.request().adaptTo(RenderedResource.class);
        List<String> supportedRenditions = rendered.getSupportedRenditions();
        assertNotNull(supportedRenditions);
        assertEquals(3, supportedRenditions.size());
        assertEquals("test", supportedRenditions.get(0));
        assertEquals("test2.png", supportedRenditions.get(1));
        assertEquals("test2.jpeg", supportedRenditions.get(2));
    }

    @Test
    public void testSlingHttpServletRequest() {
        addRendition("/content/slingfile.jpg", "test.png");
        addRendition("/content/slingfile.jpg", "test2.png");
        addRendition("/content/slingfile.jpg", "test2.jpeg");
        RenderedResource rendered = context.request().adaptTo(RenderedResource.class);
        context.request().addRequestParameter("src", slingFileResource.getPath());
        List<String> supportedRenditions = rendered.getSupportedRenditions();
        assertNotNull(supportedRenditions);
        assertEquals(3, supportedRenditions.size());
        assertEquals("test", supportedRenditions.get(0));
        assertEquals("test2.png", supportedRenditions.get(1));
        assertEquals("test2.jpeg", supportedRenditions.get(2));
    }

    @Test
    public void testSlingHttpServletRequestNoSrc() {
        addRendition("/content/slingfile.jpg", "test.png");
        addRendition("/content/slingfile.jpg", "test2.png");
        addRendition("/content/slingfile.jpg", "test2.jpeg");
        ((MockRequestPathInfo) context.request().getRequestPathInfo()).setSuffix(slingFileResource.getPath());
        RenderedResource rendered = context.request().adaptTo(RenderedResource.class);
        List<String> supportedRenditions = rendered.getSupportedRenditions();
        assertNotNull(supportedRenditions);
        assertEquals(3, supportedRenditions.size());
        assertEquals("test", supportedRenditions.get(0));
        assertEquals("test2.png", supportedRenditions.get(1));
        assertEquals("test2.jpeg", supportedRenditions.get(2));
    }
}
