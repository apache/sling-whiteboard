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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.apache.sling.thumbnails.extension.TransformationHandler;
import org.apache.sling.thumbnails.internal.providers.ImageThumbnailProvider;
import org.apache.sling.thumbnails.internal.providers.PdfThumbnailProvider;
import org.apache.sling.thumbnails.internal.transformers.CropHandler;
import org.apache.sling.thumbnails.internal.transformers.ResizeHandler;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ThumbnailsWebConsoleTest {

    private ThumbnailsWebConsole wc;

    @Rule
    public final SlingContext context = new SlingContext();

    @Before
    public void init() throws IllegalAccessException, LoginException {

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
        wc = new ThumbnailsWebConsole(thumbnailSupport, transformer);

    }

    @Test
    public void testWebConsole() throws IOException, ServletException {

        String expected = IOUtils.toString(
                ThumbnailsWebConsoleTest.class.getClassLoader().getResourceAsStream("web-console.txt"),
                StandardCharsets.UTF_8);
        wc.renderContent(context.request(), context.response());

        assertEquals(expected, context.response().getOutputAsString());
    }

    @Test
    public void testTitle() throws IOException, ServletException {

        assertEquals("Sling Thumbnails", wc.getTitle());
    }

    @Test
    public void testLabel() throws IOException, ServletException {
        assertEquals("thumbnails", wc.getLabel());
    }

}
