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
package org.apache.sling.feature.extension.unpack.impl;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.extension.unpack.Unpack;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class UnpackArchiveInstallerPluginTest {
    @Test
    public void testCreatePlugin() throws Exception {
        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getProperty(UnpackArchiveExtensionHandler.UNPACK_EXTENSIONS_PROP))
            .thenReturn("foobar;dir:=/abc;default:=true");

        UnpackArchiveInstallerPlugin aeip = new UnpackArchiveInstallerPlugin(bc);

        Unpack unpack = aeip.unpack;
        Field dmf = Unpack.class.getDeclaredField("defaultMapping");
        dmf.setAccessible(true);
        assertEquals("foobar", dmf.get(unpack));
    }

    @Test
    public void testNoTransform() {
        Unpack unpack = Mockito.mock(Unpack.class);
        UnpackArchiveInstallerPlugin aeip = new UnpackArchiveInstallerPlugin(unpack);

        RegisteredResource res0 = Mockito.mock(RegisteredResource.class);
        assertNull(aeip.transform(res0));

        RegisteredResource res1 = Mockito.mock(RegisteredResource.class);
        Mockito.when(res1.getType()).thenReturn("Toast");
        assertNull(aeip.transform(res1));

        RegisteredResource res2 = Mockito.mock(RegisteredResource.class);
        Mockito.when(res2.getType()).thenReturn(InstallableResource.TYPE_FILE);
        assertNull(aeip.transform(res2));
    }

    @Test
    public void testTransform() throws Exception {
        ArtifactId aid = ArtifactId.fromMvnId("g:a:9");
        Hashtable <String,Object> props = new Hashtable<>();
        props.put("dir", "/some/where");
        props.put("artifact.id", aid);

        InputStream bais = new ByteArrayInputStream("".getBytes());

        Unpack unpack = Mockito.mock(Unpack.class);
        Mockito.when(unpack.handles(bais, props)).thenReturn(true);

        UnpackArchiveInstallerPlugin aeip = new UnpackArchiveInstallerPlugin(unpack);

        RegisteredResource res = Mockito.mock(RegisteredResource.class);
        Mockito.when(res.getDictionary()).thenReturn(props);
        Mockito.when(res.getInputStream()).thenReturn(bais);
        Mockito.when(res.getType()).thenReturn(InstallableResource.TYPE_FILE);
        TransformationResult[] tra = aeip.transform(res);

        assertEquals(1, tra.length);
        TransformationResult tr = tra[0];

        assertEquals(UnpackArchiveInstallerPlugin.TYPE_UNPACK_ARCHIVE, tr.getResourceType());
        assertEquals("g:a", tr.getId());
        assertSame(bais, tr.getInputStream());

        Map<String,Object> ctx = tr.getAttributes();
        assertSame(ctx, ctx.get("context"));

        for (Map.Entry<String,Object> entry : props.entrySet()) {
            assertEquals(entry.getValue(), ctx.get(entry.getKey()));
        }
    }

    @Test
    public void testTransformConstructArtifactID() throws Exception {
        Hashtable <String,Object> props = new Hashtable<>();

        InputStream bais = new ByteArrayInputStream("".getBytes());

        Unpack unpack = Mockito.mock(Unpack.class);
        Mockito.when(unpack.handles(bais, props)).thenReturn(true);

        UnpackArchiveInstallerPlugin aeip = new UnpackArchiveInstallerPlugin(unpack);

        RegisteredResource res = Mockito.mock(RegisteredResource.class);
        Mockito.when(res.getDictionary()).thenReturn(props);
        Mockito.when(res.getInputStream()).thenReturn(bais);
        Mockito.when(res.getType()).thenReturn(InstallableResource.TYPE_FILE);
        Mockito.when(res.getURL()).thenReturn("myscheme:/somefilename.bin");
        Mockito.when(res.getDigest()).thenReturn("digestive!");
        TransformationResult[] tra = aeip.transform(res);

        assertEquals(1, tra.length);
        TransformationResult tr = tra[0];

        assertEquals(UnpackArchiveInstallerPlugin.TYPE_UNPACK_ARCHIVE, tr.getResourceType());
        assertEquals("unpack.packages:somefilename", tr.getId());
        assertSame(bais, tr.getInputStream());

        Map<String,Object> ctx = tr.getAttributes();
        assertSame(ctx, ctx.get("context"));
    }
}
