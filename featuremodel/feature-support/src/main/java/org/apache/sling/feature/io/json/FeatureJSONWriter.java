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
package org.apache.sling.feature.io.json;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Include;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import static org.apache.sling.feature.support.util.ManifestUtil.marshalAttribute;
import static org.apache.sling.feature.support.util.ManifestUtil.marshalDirective;

/**
 * Simple JSON writer for a feature
 */
public class FeatureJSONWriter extends JSONWriterBase {
    private FeatureJSONWriter() {}

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

    private void writeProperty(final JsonObjectBuilder ob, final String key, final String value) {
        if ( value != null ) {
            ob.add(key, value);
        }
    }

    private void writeFeature(final Writer writer, final Feature feature)
    throws IOException {
        JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add(JSONConstants.FEATURE_ID, feature.getId().toMvnId());

        // title, description, vendor, license
        writeProperty(ob, JSONConstants.FEATURE_TITLE, feature.getTitle());
        writeProperty(ob, JSONConstants.FEATURE_DESCRIPTION, feature.getDescription());
        writeProperty(ob, JSONConstants.FEATURE_VENDOR, feature.getVendor());
        writeProperty(ob, JSONConstants.FEATURE_LICENSE, feature.getLicense());

        // variables
        writeVariables(ob, feature.getVariables());

        // includes
        if ( !feature.getIncludes().isEmpty() ) {
            JsonArrayBuilder incArray = Json.createArrayBuilder();
            for(final Include inc : feature.getIncludes()) {
                if ( inc.getArtifactExtensionRemovals().isEmpty()
                     && inc.getBundleRemovals().isEmpty()
                     && inc.getConfigurationRemovals().isEmpty()
                     && inc.getFrameworkPropertiesRemovals().isEmpty() ) {
                    incArray.add(inc.getId().toMvnId());
                } else {
                    JsonObjectBuilder includeObj = Json.createObjectBuilder();
                    includeObj.add(JSONConstants.ARTIFACT_ID, inc.getId().toMvnId());

                    JsonObjectBuilder removalsObj = Json.createObjectBuilder();
                    if ( !inc.getArtifactExtensionRemovals().isEmpty()
                         || inc.getExtensionRemovals().isEmpty() ) {
                        JsonArrayBuilder extRemovals = Json.createArrayBuilder();
                        for(final String id : inc.getExtensionRemovals()) {
                            extRemovals.add(id);
                        }
                        for(final Map.Entry<String, List<ArtifactId>> entry : inc.getArtifactExtensionRemovals().entrySet()) {
                            JsonArrayBuilder ab = Json.createArrayBuilder();
                            for(final ArtifactId id : entry.getValue()) {
                                ab.add(id.toMvnId());
                            }
                            extRemovals.add(Json.createObjectBuilder().add(entry.getKey(),
                                    ab.build()).build());
                        }
                        removalsObj.add(JSONConstants.INCLUDE_EXTENSION_REMOVALS, extRemovals.build());
                    }
                    if ( !inc.getConfigurationRemovals().isEmpty() ) {
                        JsonArrayBuilder cfgRemovals = Json.createArrayBuilder();
                        for(final String val : inc.getConfigurationRemovals()) {
                            cfgRemovals.add(val);
                        }
                        removalsObj.add(JSONConstants.FEATURE_CONFIGURATIONS, cfgRemovals.build());
                    }
                    if ( !inc.getBundleRemovals().isEmpty() ) {
                        JsonArrayBuilder bundleRemovals = Json.createArrayBuilder();
                        for(final ArtifactId val : inc.getBundleRemovals()) {
                            bundleRemovals.add(val.toMvnId());
                        }
                        removalsObj.add(JSONConstants.FEATURE_BUNDLES, bundleRemovals.build());
                    }
                    if ( !inc.getFrameworkPropertiesRemovals().isEmpty() ) {
                        JsonArrayBuilder propRemovals = Json.createArrayBuilder();
                        for(final String val : inc.getFrameworkPropertiesRemovals()) {
                            propRemovals.add(val);
                        }
                        removalsObj.add(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES, propRemovals.build());
                    }
                    includeObj.add(JSONConstants.INCLUDE_REMOVALS, removalsObj.build());

                    incArray.add(includeObj.build());
                }
            }
            ob.add(JSONConstants.FEATURE_INCLUDES, incArray.build());
        }

        // requirements
        if ( !feature.getRequirements().isEmpty() ) {
            JsonArrayBuilder requirements = Json.createArrayBuilder();

            for(final Requirement req : feature.getRequirements()) {
                JsonObjectBuilder requirementObj = Json.createObjectBuilder();
                requirementObj.add(JSONConstants.REQCAP_NAMESPACE, req.getNamespace());
                if ( !req.getAttributes().isEmpty() ) {
                    JsonObjectBuilder attrObj = Json.createObjectBuilder();
                    req.getAttributes().forEach((key, value) -> marshalAttribute(key, value, attrObj::add));
                    requirementObj.add(JSONConstants.REQCAP_ATTRIBUTES, attrObj.build());
                }
                if ( !req.getDirectives().isEmpty() ) {
                    JsonObjectBuilder reqObj = Json.createObjectBuilder();
                    req.getDirectives().forEach((key, value) -> marshalDirective(key, value, reqObj::add));
                    requirementObj.add(JSONConstants.REQCAP_DIRECTIVES, reqObj.build());
                }
                requirements.add(requirementObj.build());
            }
            ob.add(JSONConstants.FEATURE_REQUIREMENTS, requirements.build());
        }

        // capabilities
        if ( !feature.getCapabilities().isEmpty() ) {
            JsonArrayBuilder capabilities = Json.createArrayBuilder();

            for(final Capability cap : feature.getCapabilities()) {
                JsonObjectBuilder capabilityObj = Json.createObjectBuilder();
                capabilityObj.add(JSONConstants.REQCAP_NAMESPACE, cap.getNamespace());
                if ( !cap.getAttributes().isEmpty() ) {
                    JsonObjectBuilder attrObj = Json.createObjectBuilder();
                    cap.getAttributes().forEach((key, value) -> marshalAttribute(key, value, attrObj::add));
                    capabilityObj.add(JSONConstants.REQCAP_ATTRIBUTES, attrObj.build());
                }
                if ( !cap.getDirectives().isEmpty() ) {
                    JsonObjectBuilder reqObj = Json.createObjectBuilder();
                    cap.getDirectives().forEach((key, value) -> marshalDirective(key, value, reqObj::add));
                    capabilityObj.add(JSONConstants.REQCAP_DIRECTIVES, reqObj.build());
                }
                capabilities.add(capabilityObj.build());
            }
            ob.add(JSONConstants.FEATURE_CAPABILITIES, capabilities.build());
        }

        // bundles
        writeBundles(ob, feature.getBundles(), feature.getConfigurations());

        // configurations
        final Configurations cfgs = new Configurations();
        for(final Configuration cfg : feature.getConfigurations()) {
            final String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT);
            if (  artifactProp == null ) {
                cfgs.add(cfg);
            }
        }
        writeConfigurations(ob, cfgs);

        // framework properties
        writeFrameworkProperties(ob, feature.getFrameworkProperties());

        // extensions
        writeExtensions(ob, feature.getExtensions(), feature.getConfigurations());

        JsonWriterFactory writerFactory = Json.createWriterFactory(
                Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        JsonWriter jw = writerFactory.createWriter(writer);
        jw.writeObject(ob.build());
        jw.close();
    }
}
