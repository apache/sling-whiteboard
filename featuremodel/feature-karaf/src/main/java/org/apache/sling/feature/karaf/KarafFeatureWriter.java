/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.karaf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.support.ConfigurationUtil;
import org.apache.sling.feature.support.artifact.ArtifactHandler;
import org.apache.sling.feature.support.artifact.ArtifactManager;


/**
 * This writer writes out a Karaf feature XML.
 *
 */
public class KarafFeatureWriter {

    public static void writeKAR(final OutputStream os,
            final Feature feature,
            final ArtifactManager artifactManager)
    throws IOException {
        // check for repoinit extension
        final Extension repoinitExt = feature.getExtensions().getByName(Extension.NAME_REPOINIT);
        File configurationBundleFile = null;
        Artifact configuratorBundle = null;
        try {
            final Configurations configs = new Configurations();
            configs.addAll(feature.getConfigurations());
            if ( repoinitExt != null ) {
                final Configuration cfg = new Configuration("org.apache.sling.jcr.repoinit.RepositoryInitializer",
                        "feature-" + feature.getId().getArtifactId());
                cfg.getProperties().put("scripts", repoinitExt.getText());
                configs.add(cfg);
            }
            if ( !configs.isEmpty() ) {
                configurationBundleFile = Files.createTempFile(null, null).toFile();

                try ( final FileOutputStream fos = new FileOutputStream(configurationBundleFile)) {
                    final Map<String, String> map;
                    if ( repoinitExt == null ) {
                        map = null;
                    } else {
                        map = Collections.singletonMap("Require-Capability",
                                ConfigurationUtil.REQUIRE_REPOINIT_CAPABILITY);
                    }
                    ConfigurationUtil.createConfiguratorBundle(os,
                            configs,
                            feature.getId().getGroupId() + "." + feature.getId().getArtifactId(),
                            feature.getId().getOSGiVersion().toString(),
                            map);
                    configuratorBundle = new Artifact(new ArtifactId(feature.getId().getGroupId(),
                            feature.getId().getArtifactId(),
                            feature.getId().getVersion(), "configurator", null));
                }
            }

            try ( final ZipOutputStream jos = new ZipOutputStream(os) ) {
                // repository/features.xml
                // repository/{maven-path-to-bundle}
                final ZipEntry xmlEntry = new ZipEntry("repository/features.xml");
                jos.putNextEntry(xmlEntry);

                final Writer writer = new OutputStreamWriter(jos);
                writeFeaturesXML(writer, feature, configuratorBundle);
                writer.flush();

                jos.closeEntry();

                for(final Map.Entry<Integer, List<Artifact>> entry : feature.getBundles().getBundlesByStartOrder().entrySet()) {
                    for(final Artifact artifact : entry.getValue()) {
                        final ArtifactHandler handler = artifactManager.getArtifactHandler(artifact.getId().toMvnUrl());

                        addEntry(jos, artifact, handler.getFile());
                    }
                }

                if ( configuratorBundle != null ) {
                    addEntry(jos, configuratorBundle, configurationBundleFile);
                }
            }
        } finally {
            if ( configurationBundleFile != null ) {
                configurationBundleFile.delete();
            }
        }
    }

    private static void addEntry(final ZipOutputStream jos, final Artifact artifact, final File file)
    throws IOException {
        final ZipEntry bundleEntry = new ZipEntry("repository/" + artifact.getId().toMvnPath());
        jos.putNextEntry(bundleEntry);

        final byte[] buffer = new byte[16384];
        try ( final FileInputStream fis = new FileInputStream(file)) {
            int l = 0;
            while ( (l = fis.read(buffer)) > 0 ) {
                jos.write(buffer, 0, l);
            }
        }

        jos.closeEntry();
    }

    /**
     * Writes the feature XML to the writer.
     * The writer is not closed.
     * @param writer Writer
     * @param model Model
     * @throws IOException
     */
    private static void writeFeaturesXML(final Writer writer,
            final Feature feature,
            final Artifact configuratorBundle)
    throws IOException {
        final PrintWriter w = new PrintWriter(writer);

        w.print("<features name=\"");
        w.print(feature.getId().getArtifactId());
        w.print("-repo-");
        w.print(feature.getId().getVersion());
        w.println("\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.4.0\">");

        write(w, feature, configuratorBundle);

        w.println("</features>");
        w.flush();
    }

    private static void write(final PrintWriter w, final Feature feature, final Artifact configuratorBundle)
    throws IOException {
        w.print("  <feature name=\"");
        w.print(feature.getId().getGroupId());
        w.print('.');
        w.print(feature.getId().getArtifactId());
        if ( feature.getId().getClassifier() != null ) {
            w.print('.');
            w.print(feature.getId().getClassifier());
        }
        w.print("\" version=\"");
        w.print(feature.getId().getVersion());
        w.println("\">");

        if ( configuratorBundle != null ) {
            w.print("     <bundle start-level=\"1\">");
            w.print(configuratorBundle.getId().toMvnUrl());
            w.println("</bundle>");
        }

        // bundles
        for(final Map.Entry<Integer, List<Artifact>> entry : feature.getBundles().getBundlesByStartOrder().entrySet()) {
            for(final Artifact artifact : entry.getValue()) {
                w.print("     <bundle start-level=\"");
                w.print(entry.getKey().toString());
                w.print("\">");
                w.print(artifact.getId().toMvnUrl());
                w.println("</bundle>");
            }
        }

        w.println("  </feature>");
    }
}
