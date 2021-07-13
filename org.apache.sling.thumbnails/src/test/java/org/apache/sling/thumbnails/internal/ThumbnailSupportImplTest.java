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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;

import org.junit.Test;

public class ThumbnailSupportImplTest {

    @Test
    public void testValidConfig() {
        ThumbnailSupportImpl support = new ThumbnailSupportImpl(new ThumbnailSupportConfig() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String[] supportedTypes() {
                return new String[] { "nt:file=jcr:content/jcr:mimeType", "willbe:skipped",
                        "sling:File=jcr:content/jcr:mimeType2" };
            }

            @Override
            public String[] persistableTypes() {
                return new String[] { "sling:File=jcr:content/renditions", "willbe:skipped" };
            }

            @Override
            public String errorResourcePath() {
                return "/content/error";
            }

            @Override
            public String errorSuffix() {
                return "errorsuffix";
            }

        });

        assertEquals(2, support.getSupportedTypes().size());
        assertTrue(support.getSupportedTypes().contains("nt:file"));
        assertTrue(support.getSupportedTypes().contains("sling:File"));

        try {
            support.getMetaTypePropertyPath("nt:folder");
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertEquals("jcr:content/jcr:mimeType", support.getMetaTypePropertyPath("nt:file"));
        assertEquals("jcr:content/jcr:mimeType2", support.getMetaTypePropertyPath("sling:File"));

        assertEquals(1, support.getPersistableTypes().size());
        assertTrue(support.getPersistableTypes().contains("sling:File"));
        assertEquals("jcr:content/renditions", support.getRenditionPath("sling:File"));

        try {
            support.getRenditionPath("nt:file");
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }

        assertEquals("/content/error", support.getServletErrorResourcePath());
        assertEquals("errorsuffix", support.getServletErrorSuffix());
    }

    @Test
    public void testHandleDuplicatesAndUnsupported() {
        ThumbnailSupportImpl support = new ThumbnailSupportImpl(new ThumbnailSupportConfig() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String[] supportedTypes() {
                return new String[] { "nt:file=jcr:content/jcr:mimeType", "nt:file=jcr:content/jcr:mimeType2" };
            }

            @Override
            public String[] persistableTypes() {
                return new String[] { "nt:file=SOMEWHERE", "nt:file=RAINBOW", "sling:File=jcr:content/renditions",
                        "sling:File=NOWHERE" };
            }

            @Override
            public String errorResourcePath() {
                return "/content/error";
            }

            @Override
            public String errorSuffix() {
                return "errorsuffix";
            }

        });

        assertEquals(1, support.getSupportedTypes().size());
        assertEquals("jcr:content/jcr:mimeType", support.getMetaTypePropertyPath("nt:file"));

        assertEquals(1, support.getPersistableTypes().size());
        assertTrue(support.getPersistableTypes().contains("nt:file"));
        assertEquals("SOMEWHERE", support.getRenditionPath("nt:file"));

    }
}
