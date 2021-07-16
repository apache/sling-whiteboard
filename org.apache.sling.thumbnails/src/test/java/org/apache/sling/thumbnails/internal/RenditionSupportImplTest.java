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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.thumbnails.RenditionSupport;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RenditionSupportImplTest {

    private RenditionSupport renditionSupport;

    @Rule
    public final SlingContext context = new SlingContext();

    private @NotNull Resource slingFolderResource;

    private @NotNull Resource ntFileresource;

    private @NotNull Resource slingFileResource;

    private TransformationServiceUser tsu;

    @Before
    public void init() throws IllegalAccessException, LoginException {

        ContextHelper.initContext(context);

        ThumbnailSupport thumbnailSupport = mock(ThumbnailSupport.class);
        when(thumbnailSupport.getPersistableTypes()).thenReturn(Collections.singleton("sling:File"));
        when(thumbnailSupport.getRenditionPath("sling:File")).thenReturn("jcr:content/renditions");

        Set<String> supportedTypes = new HashSet<>();
        supportedTypes.add("sling:File");
        supportedTypes.add("nt:file");
        when(thumbnailSupport.getSupportedTypes()).thenReturn(supportedTypes);
        when(thumbnailSupport.getMetaTypePropertyPath(anyString())).thenReturn("jcr:content/jcr:mimeType");

        tsu = mock(TransformationServiceUser.class);
        when(tsu.getTransformationServiceUser()).thenReturn(context.resourceResolver());

        renditionSupport = new RenditionSupportImpl(thumbnailSupport, tsu);

        slingFolderResource = context.resourceResolver().getResource("/content");
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

    }

    @Test
    public void testSupportsRenditions() {
        assertFalse(renditionSupport.supportsRenditions(ntFileresource));
        assertFalse(renditionSupport.supportsRenditions(slingFolderResource));
        assertTrue(renditionSupport.supportsRenditions(slingFileResource));
    }

    @Test
    public void testRenditionExists() {
        assertFalse(renditionSupport.renditionExists(ntFileresource, "myrendition.png"));

        ntFileresource = context.create().resource("/content/ntfile.jpg/jcr:content/renditions",
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, "sling:Folder"));

        Map<String, Object> ntFileProperties = new HashMap<>();
        ntFileProperties.put("jcr:primaryType", JcrConstants.NT_FILE);
        ntFileProperties.put("jcr:content/jcr:primaryType", JcrConstants.NT_RESOURCE);
        ntFileProperties.put("jcr:content/jcr:data", new byte[] { 1, 0 });
        ntFileProperties.put("jcr:content/jcr:mimeType", "image/png");
        context.create().resource("/content/slingfile.jpg/jcr:content/renditions/myrendition.png", ntFileProperties);

        assertTrue(renditionSupport.renditionExists(slingFileResource, "myrendition.png"));
        assertFalse(renditionSupport.renditionExists(slingFileResource, "myrendition.jpg"));
        assertFalse(renditionSupport.renditionExists(slingFileResource, "myrendition2.ong"));
    }

    @Test
    public void testCreateRendition() throws PersistenceException {
        assertFalse(renditionSupport.renditionExists(slingFileResource, "myrendition.png"));
        assertNull(renditionSupport.getRenditionContent(slingFileResource, "myrendition.png"));
        renditionSupport.setRendition(slingFileResource, "myrendition.png",
                new ByteArrayInputStream(new byte[] { 0, 1 }));
        assertTrue(renditionSupport.renditionExists(slingFileResource, "myrendition.png"));
        assertNotNull(renditionSupport.getRenditionContent(slingFileResource, "myrendition.png"));
    }

    @Test
    public void testListRenditions() throws PersistenceException {
        renditionSupport.setRendition(slingFileResource, "myrendition.png",
                new ByteArrayInputStream(new byte[] { 0, 1 }));
        renditionSupport.setRendition(slingFileResource, "myrendition.jpeg",
                new ByteArrayInputStream(new byte[] { 0, 1 }));
        context.create().resource(slingFileResource.getPath() + "/jcr:content/renditions/jcr:content");
        assertNotNull(renditionSupport.listRenditions(slingFileResource));
        assertEquals(2, renditionSupport.listRenditions(slingFileResource).size());
        assertNotNull(renditionSupport.listRenditions(ntFileresource));
    }

    @Test(expected = PersistenceException.class)
    public void testLoginFailure() throws PersistenceException, LoginException {

        when(tsu.getTransformationServiceUser()).thenThrow(new LoginException("I'm sorry, I can't do that Dave"));
        renditionSupport.setRendition(slingFileResource, "myrendition.png",
                new ByteArrayInputStream(new byte[] { 0, 1 }));
    }
}
