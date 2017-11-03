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
package org.apache.sling.feature.launcher.impl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.launcher.impl.LauncherConfig.StartupMode;
import org.apache.sling.feature.support.ArtifactHandler;
import org.apache.sling.feature.support.ArtifactManager;
import org.apache.sling.feature.support.FeatureUtil;
import org.apache.sling.feature.support.json.ApplicationJSONReader;
import org.apache.sling.feature.support.json.ApplicationJSONWriter;

public class FeatureProcessor {

    /**
     * Initialize the launcher
     * Read the features and prepare the application
     * @param config The current configuration
     * @param artifactManager The artifact manager
     */
    public static Application createApplication(final LauncherConfig config,
            final ArtifactManager artifactManager)
    throws IOException {
        final Application app;
        if ( config.getApplicationFile() != null ) {
            String absoluteArg = config.getApplicationFile();
            if ( absoluteArg.indexOf(":") < 2 ) {
                absoluteArg = new File(absoluteArg).getAbsolutePath();
            }
            final ArtifactHandler appArtifact = artifactManager.getArtifactHandler(absoluteArg);

            try (final FileReader r = new FileReader(appArtifact.getFile())) {
                app = ApplicationJSONReader.read(r);
            }

        } else {
           app = FeatureUtil.assembleApplication(null, artifactManager, FeatureUtil.getFeatureFiles(config.getHomeDirectory(), config.getFeatureFiles()).toArray(new String[0]));
        }

        // write application back
        final File file = new File(config.getHomeDirectory(), "resources" + File.separatorChar + "provisioning" + File.separatorChar + "application.json");
        file.getParentFile().mkdirs();

        try (final FileWriter writer = new FileWriter(file)) {
            ApplicationJSONWriter.write(writer, app);
        } catch ( final IOException ioe) {
            Main.LOG().error("Error while writing application file: {}", ioe.getMessage(), ioe);
            System.exit(1);
        }

        return app;
    }

    /**
     * Prepare the launcher
     * - add all bundles to the bundle map of the installation object
     * - add all other artifacts to the install directory (only if startup mode is INSTALL)
     * - process configurations
     */
    public static void prepareLauncher(final LauncherConfig config,
            final ArtifactManager artifactManager,
            final Application app) throws Exception {
        for(final Map.Entry<Integer, List<Artifact>> entry : app.getBundles().getBundlesByStartLevel().entrySet()) {
            for(final Artifact a : entry.getValue()) {
                final ArtifactHandler handler = artifactManager.getArtifactHandler(":" + a.getId().toMvnPath());
                final File artifactFile = handler.getFile();

                config.getInstallation().addBundle(entry.getKey(), artifactFile);
            }
        }
        int index = 1;
        for(final Extension ext : app.getExtensions()) {
            if ( ext.getType() == ExtensionType.ARTIFACTS ) {
                for(final Artifact a : ext.getArtifacts() ) {
                    if ( config.getStartupMode() == StartupMode.PURE ) {
                        throw new Exception("Artifacts other than bundle are not supported by framework launcher.");
                    }
                    final ArtifactHandler handler = artifactManager.getArtifactHandler(":" + a.getId().toMvnPath());
                    config.getInstallation().addInstallableArtifact(handler.getFile());
                }
            } else {
                if ( ext.getName().equals(Extension.NAME_REPOINIT) ) {
                    if ( ext.getType() != ExtensionType.TEXT ) {
                        throw new Exception(Extension.NAME_REPOINIT + " extension must be of type text and not json");
                    }
                    final Configuration cfg = new Configuration("org.apache.sling.jcr.repoinit.RepositoryInitializer", "repoinit" + String.valueOf(index));
                    index++;
                    cfg.getProperties().put("scripts", ext.getText());
                    config.getInstallation().addConfiguration(cfg.getName(), cfg.getFactoryPid(), cfg.getProperties());
                } else {
                    if ( ext.isRequired() ) {
                        throw new Exception("Unknown required extension " + ext.getName());
                    }
                }
            }
        }

        for(final Configuration cfg : app.getConfigurations()) {
            if ( cfg.isFactoryConfiguration() ) {
                config.getInstallation().addConfiguration(cfg.getName(), cfg.getFactoryPid(), cfg.getProperties());
            } else {
                config.getInstallation().addConfiguration(cfg.getPid(), null, cfg.getProperties());
            }
        }

        for(final Map.Entry<String, String> prop : app.getFrameworkProperties()) {
            if ( !config.getInstallation().getFrameworkProperties().containsKey(prop.getKey()) ) {
                config.getInstallation().getFrameworkProperties().put(prop.getKey(), prop.getValue());
            }
        }
    }
}
