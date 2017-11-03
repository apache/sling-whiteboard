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
package org.apache.sling.feature.support.json;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Capability;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Include;
import org.apache.sling.feature.Requirement;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import static org.apache.sling.feature.support.util.ManifestUtil.marshalAttribute;
import static org.apache.sling.feature.support.util.ManifestUtil.marshalDirective;


/**
 * Simple JSON writer for a feature
 */
public class FeatureJSONWriter extends JSONWriterBase {

    /**
     * Writes the feature to the writer.
     * The writer is not closed.
     * @param writer Writer
     * @param feature Feature
     * @throws IOException If writing fails
     */
    public static void write(final Writer writer, final Feature feature)
    throws IOException {
        final FeatureJSONWriter w = new FeatureJSONWriter();
        w.writeFeature(writer, feature);
    }

    private void writeProperty(final JsonGenerator w, final String key, final String value) {
        if ( value != null ) {
            w.write(key, value);
        }
    }

    private void writeFeature(final Writer writer, final Feature feature)
    throws IOException {
        final JsonGenerator w = Json.createGenerator(writer);
        w.writeStartObject();

        w.write(JSONConstants.FEATURE_ID, feature.getId().toMvnId());

        // title, description, vendor, license
        writeProperty(w, JSONConstants.FEATURE_TITLE, feature.getTitle());
        writeProperty(w, JSONConstants.FEATURE_DESCRIPTION, feature.getDescription());
        writeProperty(w, JSONConstants.FEATURE_VENDOR, feature.getVendor());
        writeProperty(w, JSONConstants.FEATURE_LICENSE, feature.getLicense());

        // upgradeOf
        if ( feature.getUpgradeOf() != null ) {
            writeProperty(w, JSONConstants.FEATURE_UPGRADEOF, feature.getUpgradeOf().toMvnId());
        }

        // upgrades
        if ( !feature.getUpgrades().isEmpty() ) {
            w.writeStartArray(JSONConstants.FEATURE_UPGRADES);
            for(final ArtifactId id : feature.getUpgrades()) {
                w.write(id.toMvnId());
            }
            w.writeEnd();
        }

        // includes
        if ( !feature.getIncludes().isEmpty() ) {
            w.writeStartArray(JSONConstants.FEATURE_INCLUDES);

            for(final Include inc : feature.getIncludes()) {
                if ( inc.getArtifactExtensionRemovals().isEmpty()
                     && inc.getBundleRemovals().isEmpty()
                     && inc.getConfigurationRemovals().isEmpty()
                     && inc.getFrameworkPropertiesRemovals().isEmpty() ) {
                    w.write(inc.getId().toMvnId());
                } else {
                    w.writeStartObject();
                    w.write(JSONConstants.ARTIFACT_ID, inc.getId().toMvnId());
                    w.writeStartObject(JSONConstants.INCLUDE_REMOVALS);
                    if ( !inc.getArtifactExtensionRemovals().isEmpty()
                         || inc.getExtensionRemovals().isEmpty() ) {
                        w.writeStartArray(JSONConstants.INCLUDE_EXTENSION_REMOVALS);
                        for(final String id : inc.getExtensionRemovals()) {
                            w.write(id);
                        }
                        for(final Map.Entry<String, List<ArtifactId>> entry : inc.getArtifactExtensionRemovals().entrySet()) {
                            w.writeStartObject(entry.getKey());
                            w.writeStartArray();
                            for(final ArtifactId id : entry.getValue()) {
                                w.write(id.toMvnId());
                            }
                            w.writeEnd();
                            w.writeEnd();
                        }
                        w.writeEnd();
                    }
                    if ( !inc.getConfigurationRemovals().isEmpty() ) {
                        w.writeStartArray(JSONConstants.FEATURE_CONFIGURATIONS);
                        for(final String val : inc.getConfigurationRemovals()) {
                            w.write(val);
                        }
                        w.writeEnd();
                    }
                    if ( !inc.getBundleRemovals().isEmpty() ) {
                        w.writeStartArray(JSONConstants.FEATURE_BUNDLES);
                        for(final ArtifactId val : inc.getBundleRemovals()) {
                            w.write(val.toMvnId());
                        }
                        w.writeEnd();
                    }
                    if ( !inc.getFrameworkPropertiesRemovals().isEmpty() ) {
                        w.writeStartArray(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES);
                        for(final String val : inc.getFrameworkPropertiesRemovals()) {
                            w.write(val);
                        }
                        w.writeEnd();
                    }
                    w.writeEnd();
                    w.writeEnd();
                }
            }
            w.writeEnd();
        }

        // requirements
        if ( !feature.getRequirements().isEmpty() ) {
            w.writeStartArray(JSONConstants.FEATURE_REQUIREMENTS);

            for(final Requirement req : feature.getRequirements()) {
                w.writeStartObject();
                w.write(JSONConstants.REQCAP_NAMESPACE, req.getNamespace());
                if ( !req.getAttributes().isEmpty() ) {
                    w.writeStartObject(JSONConstants.REQCAP_ATTRIBUTES);
                    req.getAttributes().forEach((key, value) -> marshalAttribute(key, value, w::write));
                    w.writeEnd();
                }
                if ( !req.getDirectives().isEmpty() ) {
                    w.writeStartObject(JSONConstants.REQCAP_DIRECTIVES);
                    req.getDirectives().forEach((key, value) -> marshalDirective(key, value, w::write));
                    w.writeEnd();
                }
                w.writeEnd();
            }
            w.writeEnd();
        }

        // capabilities
        if ( !feature.getCapabilities().isEmpty() ) {
            w.writeStartArray(JSONConstants.FEATURE_CAPABILITIES);

            for(final Capability cap : feature.getCapabilities()) {
                w.writeStartObject();
                w.write(JSONConstants.REQCAP_NAMESPACE, cap.getNamespace());
                if ( !cap.getAttributes().isEmpty() ) {
                    w.writeStartObject(JSONConstants.REQCAP_ATTRIBUTES);
                    cap.getAttributes().forEach((key, value) -> marshalAttribute(key, value, w::write));
                    w.writeEnd();
                }
                if ( !cap.getDirectives().isEmpty() ) {
                    w.writeStartObject(JSONConstants.REQCAP_DIRECTIVES);
                    cap.getDirectives().forEach((key, value) -> marshalDirective(key, value, w::write));
                    w.writeEnd();
                }
                w.writeEnd();
            }
            w.writeEnd();
        }

        // bundles
        writeBundles(w, feature.getBundles(), feature.getConfigurations());

        // configurations
        final Configurations cfgs = new Configurations();
        for(final Configuration cfg : feature.getConfigurations()) {
            final String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT);
            if (  artifactProp == null ) {
                cfgs.add(cfg);
            }
        }
        writeConfigurations(w, cfgs);

        // framework properties
        writeFrameworkProperties(w, feature.getFrameworkProperties());

        // extensions
        writeExtensions(w, feature.getExtensions(), feature.getConfigurations());

        w.writeEnd();
        w.flush();
    }
}
