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
package org.apache.sling.installer.factory.model.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;
import org.apache.sling.feature.extension.apiregions.launcher.LauncherProperties;
import org.apache.sling.feature.io.archive.ArchiveReader;
import org.apache.sling.feature.io.artifacts.ArtifactHandler;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.framework.BundleContext;

/**
 * This task installs a feature model resources.
 */
public class InstallFeatureModelTask extends AbstractFeatureModelTask {

    private static final String PROP_idbsnver = "mapping.bundleid.bsnver";
    private static final String PROP_bundleFeatures = "mapping.bundleid.features";
    private static final String PROP_featureRegions = "mapping.featureid.regions";
    private static final String PROP_regionPackage = "mapping.region.packages";

    private static final String REGION_FACTORY_PID = "org.apache.sling.feature.apiregions.factory~";
    private static final String REPOINIT_FACTORY_PID = "org.apache.sling.jcr.repoinit.RepositoryInitializer~";

    private final InstallContext installContext;

    public InstallFeatureModelTask(final TaskResourceGroup group,
            final InstallContext installContext, final BundleContext bundleContext) {
        super(group, bundleContext);
        this.installContext = installContext;
    }

    @Override
    public void execute(final InstallationContext ctx) {
        try {
            final TaskResource resource = this.getResource();
            ctx.log("Installing {}", resource.getEntityId());
            final String featureJson = (String) resource.getAttribute(FeatureModelInstallerPlugin.ATTR_MODEL);
            if (featureJson == null) {
                ctx.log("Unable to install feature model resource {} : no model found", resource);
                this.getResourceGroup().setFinishState(ResourceState.IGNORED);
            } else {
                boolean success = false;
                final List<InstallableResource> result = this.transform(featureJson, resource);
                if (result == null) {
                    ctx.log("Unable to install feature model resource {} : unable to create resources", resource);
                    this.getResourceGroup().setFinishState(ResourceState.IGNORED);
                } else {
                    if (!result.isEmpty()) {
                        final OsgiInstaller installer = this.getService(OsgiInstaller.class);
                        if (installer != null) {
                            installer.registerResources(
                                    getScheme(resource),
                                    result.toArray(new InstallableResource[result.size()]));
                        } else {
                            ctx.log("Unable to install feature model resource {} : unable to get OSGi installer",
                                    resource);
                            this.getResourceGroup().setFinishState(ResourceState.IGNORED);
                            return;
                        }
                    }
                    this.getResourceGroup().setFinishState(ResourceState.INSTALLED);
                    success = true;
                }
                if ( success ) {
                    ctx.log("Installed {}", resource.getEntityId());
                }
            }
        } finally {
            this.cleanup();
        }
    }

    private File getArtifactFile(final File baseDir, final ArtifactId id) {
        return new File(baseDir, id.toMvnId().replace('/', File.separatorChar));
    }

    private List<InstallableResource> transform(final String featureJson,
            final TaskResource rsrc) {
        Feature feature = null;
        try (final Reader reader = new StringReader(featureJson)) {
            feature = FeatureJSONReader.read(reader, null);
        } catch ( final IOException ioe) {
            logger.warn("Unable to read feature model file", ioe);
        }
        if (feature == null) {
            return null;
        }

        final List<InstallableResource> result = new ArrayList<>();
        // configurations
        for (final Configuration cfg : feature.getConfigurations()) {
            result.add(new InstallableResource("/".concat(cfg.getPid()).concat(".config"), null,
                    cfg.getConfigurationProperties(), null, InstallableResource.TYPE_CONFIG, null));
        }

        // repoinit
        final Extension repoInit = feature.getExtensions().getByName(Extension.EXTENSION_NAME_REPOINIT);
        if (repoInit != null && repoInit.getType() == ExtensionType.TEXT) {
            final String configPid = REPOINIT_FACTORY_PID.concat(feature.getId().toMvnName().replace('-', '_'));
            final Dictionary<String, Object> props = new Hashtable<>();
            props.put("scripts", repoInit.getText());

            result.add(new InstallableResource("/".concat(configPid).concat(".config"), null,
                    props, null, InstallableResource.TYPE_CONFIG, null));
        }

        // extract artifacts
        if (this.installContext.storageDirectory != null) {
            final byte[] buffer = new byte[1024*1024*256];

            try ( final InputStream is = rsrc.getInputStream() ) {
                ArchiveReader.read(is, new ArchiveReader.ArtifactConsumer() {

                    @Override
                    public void consume(final ArtifactId id, final InputStream is) throws IOException {
                        final File artifactFile = getArtifactFile(installContext.storageDirectory, id);
                        if (!artifactFile.exists()) {
                            artifactFile.getParentFile().mkdirs();
                            try (final OutputStream os = new FileOutputStream(artifactFile)) {
                                int l = 0;
                                while ((l = is.read(buffer)) > 0) {
                                    os.write(buffer, 0, l);
                                }
                            }
                        }
                    }
                });
            } catch ( final IOException ioe) {
                logger.warn("Unable to extract artifacts from feature model " + feature.getId().toMvnId(), ioe);
                return null;
            }
        }

        // api regions
        final Extension regionExt = feature.getExtensions().getByName(ApiRegions.EXTENSION_NAME);
        if ( regionExt != null ) {
            try {
                final ApiRegions regions = ApiRegions.parse(regionExt.getJSONStructure().asJsonArray());

                final String configPid = REGION_FACTORY_PID.concat(feature.getId().toMvnName().replace('-', '_'));
                final Dictionary<String, Object> props = new Hashtable<>();
                props.put(PROP_idbsnver, LauncherProperties.getBundleIDtoBSNandVersionMap(feature, this.installContext.artifactManager));
                props.put(PROP_bundleFeatures, LauncherProperties.getBundleIDtoFeaturesMap(feature));
                props.put(PROP_featureRegions, LauncherProperties.getFeatureIDtoRegionsMap(regions));
                props.put(PROP_regionPackage, LauncherProperties.getRegionNametoPackagesMap(regions));

                result.add(new InstallableResource("/".concat(configPid).concat(".config"), null,
                        props, null, InstallableResource.TYPE_CONFIG, null));
            } catch (final IOException ioe) {
                logger.warn("Unable to parse region information " + feature.getId().toMvnId(), ioe);
                return null;
            }
        }

        // bundles
        for (final Artifact bundle : feature.getBundles()) {
            if (!addArtifact(bundle, result)) {
                return null;
            }
        }

        // content packages
        final Extension ext = feature.getExtensions().getByName(Extension.EXTENSION_NAME_CONTENT_PACKAGES);
        if (ext != null && ext.getType() == ExtensionType.ARTIFACTS) {
            for (final Artifact artifact : ext.getArtifacts()) {
                addArtifact(artifact, result);
            }
        }

        return result;
    }

    private boolean addArtifact(final Artifact artifact,
            final List<InstallableResource> result) {
        File artifactFile = (this.installContext.storageDirectory == null ? null
                : getArtifactFile(this.installContext.storageDirectory, artifact.getId()));
        ArtifactHandler handler;
        if (artifactFile == null || !artifactFile.exists()) {
            try {
                handler = this.installContext.artifactManager.getArtifactHandler(artifact.getId().toMvnUrl());
            } catch (final IOException ignore) {
                return false;
            }
        } else {
            try {
                handler = new ArtifactHandler(artifactFile);
            } catch (final MalformedURLException e) {
                return false;
            }
        }
        if (handler == null) {
            return false;
        }
        try {
            final URLConnection connection = handler.getLocalURL().openConnection();
            connection.connect();
            final InputStream is = connection.getInputStream();
            final long lastModified = connection.getLastModified();
            final String digest = lastModified == 0 ? null : String.valueOf(lastModified);
            // handle start order
            final Dictionary<String, Object> dict = new Hashtable<String, Object>();
            if (artifact.getStartOrder() > 0) {
                dict.put(InstallableResource.BUNDLE_START_LEVEL, artifact.getStartOrder());
            }
            dict.put(InstallableResource.RESOURCE_URI_HINT, handler.getLocalURL().toString());

            result.add(new InstallableResource("/".concat(artifact.getId().toMvnName()), is, dict, digest,
                    InstallableResource.TYPE_FILE, null));
        } catch (final IOException ioe) {
            logger.warn("Unable to read artifact " + handler.getLocalURL(), ioe);
            return false;
        }
        return true;
    }

    @Override
    public String getSortKey() {
        return "30-" + getResource().getAttribute(FeatureModelInstallerPlugin.ATTR_ID);
    }
}
