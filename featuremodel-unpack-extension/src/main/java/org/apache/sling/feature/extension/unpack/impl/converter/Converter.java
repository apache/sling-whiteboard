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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONWriter;

public class Converter {
    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            File base = new File(args[0]);
            if (!base.isDirectory() && !base.mkdirs()) {
                throw new IOException("Unable to create base dir: " + base);
            }
            Feature feature = new Feature(new ArtifactId("cm", "cm-fonts", "0.0.1", null,  "slingosgifeature"));
            Extension extension = new Extension(ExtensionType.ARTIFACTS, "user-fonts", ExtensionState.REQUIRED);
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                URL url = new URL(arg);
                File tmp = File.createTempFile("fonts", ".zip");
                try (DigestInputStream inputStream = new DigestInputStream(url.openStream(), MessageDigest.getInstance("SHA-512"))) {
                    Files.copy(inputStream, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    String digest = bytesToHex(inputStream.getMessageDigest().digest());

                    Artifact artifact = new Artifact(new ArtifactId("cm", "cm-fonts", "0.0.1", digest,  "zip"));
                    extension.getArtifacts().add(artifact);
                    File target = new File(base, artifact.getId().toMvnPath());
                    if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
                        throw new IOException("Unable to create parent dir: " + target.getParentFile());
                    }
                    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            feature.getExtensions().add(extension);
            File target = new File(base, feature.getId().toMvnPath());
            if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
                throw new IOException("Unable to create parent dir: " + target.getParentFile());
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8)) {
                FeatureJSONWriter.write(writer, feature);
            }
        }
    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes();

    static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }
}
