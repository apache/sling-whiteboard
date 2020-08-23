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
package org.apache.sling.feature.extension.unpack.impl.converter;

import java.io.File;
import java.io.FileOutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import org.apache.sling.feature.ArtifactId;
import org.junit.Assert;
import org.junit.Test;

public class ConverterTest
{
    @Test
    public void testConverter() throws Exception
    {
        File base = File.createTempFile("test", "fonts");
        base.delete();
        base.mkdirs();

        File repo = new File(base, "repository");
        File fontZip = new File(base, "fonts one-1.0.0 bar.zip");
        String digest;
        try (DigestOutputStream outputStream = new DigestOutputStream(new FileOutputStream(fontZip), MessageDigest.getInstance("SHA-512"))) {
            outputStream.write(0xff);
            digest = Converter.bytesToHex(outputStream.getMessageDigest().digest());
        }

        Converter.main(new String[]{repo.getPath(), fontZip.toURI().toURL().toString()});

        Assert.assertTrue(new File(repo, ArtifactId.fromMvnId("cm:cm-fonts:slingosgifeature:0.0.1").toMvnPath()).exists());

        Assert.assertTrue(new File(repo, ArtifactId.fromMvnId("cm:cm-fonts:zip:" + digest + ":0.0.1").toMvnPath()).exists());
    }
}
