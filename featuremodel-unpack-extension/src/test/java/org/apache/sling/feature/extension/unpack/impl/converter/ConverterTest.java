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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.extension.unpack.Unpack;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.junit.Assert;
import org.junit.Test;

public class ConverterTest
{
    @Test
    public void testConverterMain() throws Exception
    {
        File base = File.createTempFile("test", "unpack");
        base.delete();
        base.mkdirs();

        File repo = new File(base, "repository");
        File fontZip = new File(base, "unpack one-1.0.0 bar.zip");
        File target = new File(base, "feature.json");
        String digest;
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1");
        mf.getMainAttributes().putValue("FOO", "BAR");

        try (JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(fontZip), mf)) {
        }
        try (DigestInputStream inputStream = new DigestInputStream(new FileInputStream(fontZip), MessageDigest.getInstance("SHA-512"))) {
            while (inputStream.read() != -1) {

            }
            digest = Converter.bytesToHex(inputStream.getMessageDigest().digest());
        }

        Converter.main(new String[]{"org.apache.sling:sling.unpack.test:slingosgifeature:0.0.1", "unpack", target.getPath(), repo.getPath(), "value=BAR", "key=FOO", fontZip.toURI().toURL().toString()});

        Assert.assertTrue(target.isFile());

        try (Reader reader = new FileReader(target)) {
            Feature feature = FeatureJSONReader.read(reader, null);
            Assert.assertEquals(ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:slingosgifeature:0.0.1"), feature.getId());
            Assert.assertFalse(feature.getExtensions().isEmpty());
            Extension extension = feature.getExtensions().getByName("unpack");
            Assert.assertNotNull(extension);
            Assert.assertEquals(1, extension.getArtifacts().size());
            Assert.assertTrue(extension.getArtifacts().containsExact(ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:zip:" + digest + ":0.0.1")));
        }

        Assert.assertTrue(new File(repo, ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:zip:" + digest + ":0.0.1").toMvnPath()).exists());
    }

    @Test
    public void testConverter() throws Exception
    {
        File base = File.createTempFile("test", "unpack");
        base.delete();
        base.mkdirs();

        File repo = new File(base, "repository");
        File fontZip = new File(base, "unpack one-1.0.0 bar.zip");
        File target = new File(base, "feature.json");
        String digest;
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1");
        mf.getMainAttributes().putValue("FOO", "BAR");

        try (JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(fontZip), mf)) {
        }
        try (DigestInputStream inputStream = new DigestInputStream(new FileInputStream(fontZip), MessageDigest.getInstance("SHA-512"))) {
            while (inputStream.read() != -1) {

            }
            digest = Converter.bytesToHex(inputStream.getMessageDigest().digest());
        }

        List<String> unused = Converter.convert(ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:slingosgifeature:0.0.1"), "unpack", target, repo, (stream) -> Unpack.handles("FOO", "BAR", stream),  Arrays.asList(fontZip.toURI().toURL().toString()));

        Assert.assertTrue(target.isFile());
        Assert.assertTrue(unused.isEmpty());

        try (Reader reader = new FileReader(target)) {
            Feature feature = FeatureJSONReader.read(reader, null);
            Assert.assertEquals(ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:slingosgifeature:0.0.1"), feature.getId());
            Assert.assertFalse(feature.getExtensions().isEmpty());
            Extension extension = feature.getExtensions().getByName("unpack");
            Assert.assertNotNull(extension);
            Assert.assertEquals(1, extension.getArtifacts().size());
            Assert.assertTrue(extension.getArtifacts().containsExact(ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:zip:" + digest + ":0.0.1")));
        }

        Assert.assertTrue(new File(repo, ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:zip:" + digest + ":0.0.1").toMvnPath()).exists());
    }

    @Test
    public void testConverterFilter() throws Exception
    {
        File base = File.createTempFile("test", "unpack");
        base.delete();
        base.mkdirs();

        File repo = new File(base, "repository");
        File fontZip = new File(base, "unpack one-1.0.0 bar.zip");
        File target = new File(base, "feature.json");
        String digest;
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1");
        mf.getMainAttributes().putValue("FOO", "BARs");

        try (JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(fontZip), mf)) {
        }
        try (DigestInputStream inputStream = new DigestInputStream(new FileInputStream(fontZip), MessageDigest.getInstance("SHA-512"))) {
            while (inputStream.read() != -1) {

            }
            digest = Converter.bytesToHex(inputStream.getMessageDigest().digest());
        }

        List<String> unused = Converter.convert(ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:slingosgifeature:0.0.1"), "unpack", target, repo, (stream) -> Unpack.handles("FOO", "BAR", stream),  Arrays.asList(fontZip.toURI().toURL().toString()));

        Assert.assertTrue(target.isFile());
        Assert.assertFalse(unused.isEmpty());

        try (Reader reader = new FileReader(target)) {
            Feature feature = FeatureJSONReader.read(reader, null);
            Assert.assertEquals(ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:slingosgifeature:0.0.1"), feature.getId());
            Assert.assertFalse(feature.getExtensions().isEmpty());
            Extension extension = feature.getExtensions().getByName("unpack");
            Assert.assertNotNull(extension);
            Assert.assertEquals(0, extension.getArtifacts().size());
            Assert.assertFalse(extension.getArtifacts().containsExact(ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:zip:" + digest + ":0.0.1")));
        }

        Assert.assertFalse(new File(repo, ArtifactId.fromMvnId("org.apache.sling:sling.unpack.test:zip:" + digest + ":0.0.1").toMvnPath()).exists());
        Assert.assertTrue(unused.size() == 1);
        Assert.assertEquals(fontZip.toURI().toURL().toString(),unused.get(0));
    }
}
