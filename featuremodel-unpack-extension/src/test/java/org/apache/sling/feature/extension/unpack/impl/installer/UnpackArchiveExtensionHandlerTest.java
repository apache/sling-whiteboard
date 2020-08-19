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
package org.apache.sling.feature.extension.unpack.impl.installer;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.extension.unpack.Unpack;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class UnpackArchiveExtensionHandlerTest {
    @Test
    public void testCreateUnpacker() throws Exception {
        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getProperty(UnpackArchiveExtensionHandler.UNPACK_EXTENSIONS_PROP))
            .thenReturn("foobar;dir:=/abc;default:=true");

        UnpackArchiveExtensionHandler uaeh = new UnpackArchiveExtensionHandler(bc);

        Unpack unpack = uaeh.unpack;
        Field dmf = Unpack.class.getDeclaredField("defaultMapping");
        dmf.setAccessible(true);
        assertEquals("foobar", dmf.get(unpack));
    }

    @Test
    public void testCallHandler() throws Exception {
        Unpack unpack = Mockito.mock(Unpack.class);

        UnpackArchiveExtensionHandler uaeh = new UnpackArchiveExtensionHandler(unpack);

        ArtifactProvider ap = Mockito.mock(ArtifactProvider.class);

        ExtensionHandlerContext ctx = Mockito.mock(ExtensionHandlerContext.class);
        Mockito.when(ctx.getArtifactProvider()).thenReturn(ap);

        Extension ext = Mockito.mock(Extension.class);

        uaeh.handle(ctx, ext, null);

        Mockito.verify(unpack).handle(Mockito.eq(ext), Mockito.eq(ap), Mockito.notNull());
    }
}
