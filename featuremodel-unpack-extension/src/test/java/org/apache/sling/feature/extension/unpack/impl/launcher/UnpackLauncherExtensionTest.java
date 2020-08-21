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
package org.apache.sling.feature.extension.unpack.impl.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.extension.unpack.Unpack;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class UnpackLauncherExtensionTest {
    @Test
    public void testLauncherExtension() throws Exception {
        UnpackLauncherExtension handler = new UnpackLauncherExtension();

        ExtensionContext context = Mockito.mock(ExtensionContext.class);
        Map<String, String> frameworkProperties = new HashMap<>();
        File tmp = File.createTempFile("foo", "dir");
        tmp.delete();
        tmp.mkdirs();
        tmp.deleteOnExit();

        File target = new File(tmp, "target");
        target.deleteOnExit();
        frameworkProperties.put(Unpack.UNPACK_EXTENSIONS_PROP, "foo;dir:=\"" + target.getPath() + "\"");


        Mockito.when(context.getFrameworkProperties()).thenReturn(frameworkProperties);
        Artifact artifact = new Artifact(ArtifactId.fromMvnId("org.apache.sling:foo:0.1.0-SNAPSHOT"));

        File archiveJar = new File(tmp, "archive.jar");
        archiveJar.deleteOnExit();
        URL archive = createTestZip(archiveJar);

        Mockito.when(context.getArtifactFile(artifact.getId())).thenReturn(archive);

        Extension extension = new Extension(ExtensionType.ARTIFACTS, "foo", ExtensionState.REQUIRED);

        extension.getArtifacts().add(artifact);

        Assert.assertTrue(handler.handle(context, extension));

        Assert.assertTrue(new File(target, "foo/bar/baz.bin").isFile());
    }

    private URL createTestZip(File target) throws Exception {
        try (JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(target))) {
            outputStream.putNextEntry(new ZipEntry("foo/bar/baz.bin"));
            outputStream.write("this is a test".getBytes());
        }
        return target.toURI().toURL();
    }
}
