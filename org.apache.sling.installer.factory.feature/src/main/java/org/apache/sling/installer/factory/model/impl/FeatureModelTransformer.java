package org.apache.sling.installer.factory.model.impl;/*
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.archive.ArchiveReader;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.io.json.FeatureJSONWriter;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This transformer detects a file with the ending ".feature" containing a
 * feature model or a feature model archive ending in ".far"
 */
@Component(service = ResourceTransformer.class)
public class FeatureModelTransformer implements ResourceTransformer {

    public static final String FILE_EXTENSION = ".json";

    public static final String TYPE_FEATURE_MODEL = "featuremodel";

    public static final String ATTR_MODEL = "feature";

    public static final String ATTR_BASE_PATH = "path";

    public static final String ATTR_ID = "featureId";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BundleContext bundleContext;

    @Activate
    public FeatureModelTransformer(final BundleContext bc) {
        this.bundleContext = bc;
    }

    @Override
    public TransformationResult[] transform(final RegisteredResource resource) {
        Feature feature = null;
        File baseDir = null;
        if (resource.getType().equals(InstallableResource.TYPE_FILE) && resource.getURL().endsWith(FILE_EXTENSION)) {
            try ( final Reader reader = new InputStreamReader(resource.getInputStream(), "UTF-8") ) {
                feature = FeatureJSONReader.read(reader, resource.getURL());
            } catch ( final IOException ioe) {
                logger.info("Unable to read feature model from " + resource.getURL(), ioe);
            }
        }
        if (resource.getType().equals(InstallableResource.TYPE_FILE)
                && resource.getURL().endsWith(".zip")) {
            baseDir = this.bundleContext.getDataFile("");
            try ( final InputStream is = resource.getInputStream() ) {

                feature = ArchiveReader.read(is, new ArchiveReader.ArtifactConsumer() {
                    @Override
                    public void consume(final ArtifactId artifact, final InputStream is) throws IOException {
                        // nothing to do, install task does extraction
                    }
                });
            } catch ( final IOException ioe) {
                logger.info("Unable to read feature model from " + resource.getURL(), ioe);
            }
        }
        if (feature != null) {
            String featureJson = null;
            try (final StringWriter sw = new StringWriter()) {
                FeatureJSONWriter.write(sw, feature);
                featureJson = sw.toString();
            } catch (final IOException ioe) {
                logger.info("Unable to read feature model from " + resource.getURL(), ioe);
            }

            if (featureJson != null) {
                final TransformationResult tr = new TransformationResult();
                tr.setResourceType(TYPE_FEATURE_MODEL);
                tr.setId(feature.getId().toMvnId());
                tr.setVersion(feature.getId().getOSGiVersion());

                final Map<String, Object> attributes = new HashMap<>();
                attributes.put(ATTR_MODEL, featureJson);
                attributes.put(ATTR_ID, feature.getId().toMvnId());
                if (baseDir != null) {
                    final File dir = new File(baseDir, feature.getId().toMvnName());
                    attributes.put(ATTR_BASE_PATH, dir.getAbsolutePath());
                }
                tr.setAttributes(attributes);

                return new TransformationResult[] { tr };
            }
        }
        return null;
    }
}
