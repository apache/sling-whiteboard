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
package org.apache.sling.feature.extension.unpack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.sling.feature.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestUnpack
{
    @Test
    public void testUnzip() throws IOException
    {
        File tmp = File.createTempFile("foo", "dir");
        tmp.delete();
        Unpack unpack = Unpack.fromMapping("test;default:=true;dir:=\"" + tmp.getPath() + "\"");
        URL url = createZipFile("test1");

        unpack.unpack(url, Collections.emptyMap());

        Assert.assertTrue(equals("test1", url));
    }

    private URL createZipFile(String base) throws IOException {
        File tmp = File.createTempFile("foo", ".zip");
        tmp.deleteOnExit();
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1");
        mf.getMainAttributes().putValue("binary", "1");
        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmp), mf);
             BufferedReader reader = new BufferedReader(new InputStreamReader(TestUnpack.class.getResourceAsStream(base + "/index.txt"), "UTF-8")))
        {
            for (String line = reader.readLine(); line != null; line = reader.readLine())
            {
                jarOutputStream.putNextEntry(new ZipEntry(line));
                try (InputStream inputStream = TestUnpack.class.getResourceAsStream(base + "/" + line)) {
                    byte[] buffer = new byte[64 * 1024];
                    for (int i = inputStream.read(buffer); i != -1; i = inputStream.read(buffer)) {
                        jarOutputStream.write(buffer, 0, i);
                    }
                }
            }
        }
        return tmp.toURI().toURL();
    }

    private boolean equals(String base, URL jar) throws IOException {
        try (JarFile jarFile = IOUtils.getJarFileFromURL(jar, false, null);
             BufferedReader reader = new BufferedReader(new InputStreamReader(TestUnpack.class.getResourceAsStream(base + "/index.txt"), "UTF-8"))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine())
            {
                if (jarFile.getEntry(line)==null) {
                    return false;
                }
            }
        }
        return true;
    }
}
