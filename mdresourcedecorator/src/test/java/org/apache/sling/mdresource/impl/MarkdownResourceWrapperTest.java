/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.mdresource.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.mdresource.impl.ResourceConfiguration.SourceType;
import org.junit.Test;
import org.mockito.Mockito;

public class MarkdownResourceWrapperTest {

    private ResourceConfiguration newDefaultConfiguration() {
        final ResourceConfiguration cfg = new ResourceConfiguration();
        cfg.htmlProperty = "jcr:description";
        cfg.titleProperty = "jcr:title";
        cfg.sourceType = SourceType.InputStream;
        cfg.resourceType = "resource/type";
        return cfg;
    }

    @Test
    public void testMarkdown() {
        final Resource orig = Mockito.mock(Resource.class);
        Mockito.when(orig.adaptTo(InputStream.class))
            .thenReturn(this.getClass().getResourceAsStream("/md-test/index.md"));
        Mockito.when(orig.getResourceType()).thenReturn("super/type");
        Mockito.when(orig.getValueMap()).thenReturn(new ValueMapDecorator(Collections.emptyMap()));

        final ResourceConfiguration cfg = newDefaultConfiguration();

        final Resource rsrc = new MarkdownResourceWrapper(orig, cfg);

        final ValueMap map = rsrc.getValueMap();

        assertEquals("valueMap[jcr:title]", "Simple markdown file",
                map.get("jcr:title", String.class));

        assertEquals("valueMap[jcr:description]",
                "<h1>Simple markdown file</h1>\n<p>This is an example of a simple markdown file</p>\n",
                map.get("jcr:description", String.class));

        assertEquals("valueMap[author]", "John Doe", map.get("author", String.class));

        assertArrayEquals("valueMap[keywords]", new String[] {"news", "simple"},
                map.get("keywords", String[].class));

        assertEquals("valueMap[sling:resourceType]", "resource/type",
                map.get(ResourceResolver.PROPERTY_RESOURCE_TYPE));
        assertEquals("valueMap[sling:resourceSuperType]", "super/type",
                map.get("sling:resourceSuperType"));
    }

    @Test
    public void testMarkdownNoTitle() {
        final Resource orig = Mockito.mock(Resource.class);
        Mockito.when(orig.adaptTo(InputStream.class))
            .thenReturn(this.getClass().getResourceAsStream("/md-test/index.md"));
        Mockito.when(orig.getValueMap()).thenReturn(new ValueMapDecorator(Collections.emptyMap()));
        Mockito.when(orig.getResourceType()).thenReturn("super/type");

        final ResourceConfiguration cfg = newDefaultConfiguration();
        cfg.titleProperty = null;

        final Resource rsrc = new MarkdownResourceWrapper(orig, cfg);

        final ValueMap map = rsrc.getValueMap();

        assertEquals("valueMap[jcr:description]",
                "<h1>Simple markdown file</h1>\n<p>This is an example of a simple markdown file</p>\n",
                map.get("jcr:description", String.class));
    }

    @Test
    public void testResourceType() {
        final Resource orig = Mockito.mock(Resource.class);
        Mockito.when(orig.getResourceType()).thenReturn("super/type");
        Mockito.when(orig.getValueMap()).thenReturn(new ValueMapDecorator(Collections.emptyMap()));

        final ResourceConfiguration cfg = new ResourceConfiguration();
        cfg.resourceType = "resource/type";
        final Resource rsrc = new MarkdownResourceWrapper(orig, cfg);

        assertEquals("resource/type", rsrc.getResourceType());
        assertEquals("super/type", rsrc.getResourceSuperType());
    }

    @Test
    public void testTitleHeading() {
        final Resource orig = Mockito.mock(Resource.class);
        Mockito.when(orig.adaptTo(InputStream.class))
            .thenReturn(this.getClass().getResourceAsStream("/md-test/headings.md"));
        Mockito.when(orig.getResourceType()).thenReturn("super/type");
        Mockito.when(orig.getValueMap()).thenReturn(new ValueMapDecorator(Collections.emptyMap()));

        final ResourceConfiguration cfg = newDefaultConfiguration();

        final Resource rsrc = new MarkdownResourceWrapper(orig, cfg);

        final ValueMap map = rsrc.getValueMap();

        assertEquals("valueMap[jcr:title]", "First",
                map.get("jcr:title", String.class));
        assertEquals("valueMap[jcr:description]", "<h1>First</h1>\n<h1>And</h1>\n" +
                "<h2>Last</h2>\n" +
                "<h1>And</h1>\n" +
                "<h1>Always</h1>\n",
                map.get("jcr:description", String.class));

    }
}
