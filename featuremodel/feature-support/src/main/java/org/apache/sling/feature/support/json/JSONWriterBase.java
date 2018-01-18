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

import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.stream.JsonGenerator;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.KeyValueMap;


/**
 * Common functionality for writing JSON
 */
abstract class JSONWriterBase {


    protected void writeBundles(final JsonGenerator w,
            final Bundles bundles,
            final Configurations allConfigs) {
        // bundles
        if ( !bundles.getBundlesByStartOrder().isEmpty() ) {
            w.writeStartObject(JSONConstants.FEATURE_BUNDLES);
            for(final Map.Entry<Integer, List<Artifact>> entry : bundles.getBundlesByStartOrder().entrySet()) {
                w.writeStartArray(String.valueOf(entry.getKey()));

                for(final Artifact artifact : entry.getValue()) {
                    final Configurations cfgs = new Configurations();
                    for(final Configuration cfg : allConfigs) {
                        final String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT);
                        if (  artifact.getId().toMvnId().equals(artifactProp) ) {
                            cfgs.add(cfg);
                        }
                    }
                    if ( artifact.getMetadata().isEmpty() && cfgs.isEmpty() ) {
                        w.write(artifact.getId().toMvnId());
                    } else {
                        w.writeStartObject();
                        w.write(JSONConstants.ARTIFACT_ID, artifact.getId().toMvnId());

                        for(final Map.Entry<String, String> me : artifact.getMetadata()) {
                            w.write(me.getKey(), me.getValue());
                        }

                        writeConfigurations(w, cfgs);
                        w.writeEnd();
                    }
                }
                w.writeEnd();
            }

            w.writeEnd();
        }
    }

    /**
     * Write the list of configurations into a "configurations" element
     * @param w The json generator
     * @param cfgs The list of configurations
     */
    protected void writeConfigurations(final JsonGenerator w, final Configurations cfgs) {
        if ( !cfgs.isEmpty() ) {
            w.writeStartObject(JSONConstants.FEATURE_CONFIGURATIONS);

            writeConfigurationsMap(w, cfgs);

            w.writeEnd();
        }
    }

    /**
     * Write the list of configurations into a "configurations" element
     * @param w The json generator
     * @param cfgs The list of configurations
     */
    protected void writeConfigurationsMap(final JsonGenerator w, final Configurations cfgs) {
        for(final Configuration cfg : cfgs) {
            final String key;
            if ( cfg.isFactoryConfiguration() ) {
                key = cfg.getFactoryPid() + "~" + cfg.getName();
            } else {
                key = cfg.getPid();
            }
            w.writeStartObject(key);

            final Enumeration<String> e = cfg.getProperties().keys();
            while ( e.hasMoreElements() ) {
                final String name = e.nextElement();
                if ( Configuration.PROP_ARTIFACT.equals(name) ) {
                    continue;
                }

                final Object val = cfg.getProperties().get(name);

                String typePostFix = null;
                final Object typeCheck;
                if ( val.getClass().isArray() ) {
                    if ( Array.getLength(val) > 0 ) {
                        typeCheck = Array.get(val, 0);
                    } else {
                        typeCheck = null;
                    }
                } else {
                    typeCheck = val;
                }

                if ( typeCheck instanceof Integer ) {
                    typePostFix = ":Integer";
                } else if ( typeCheck instanceof Byte ) {
                    typePostFix = ":Byte";
                } else if ( typeCheck instanceof Character ) {
                    typePostFix = ":Character";
                } else if ( typeCheck instanceof Float ) {
                    typePostFix = ":Float";
                }

                if ( val.getClass().isArray() ) {
                    w.writeStartArray(name);
                    for(int i=0; i<Array.getLength(val);i++ ) {
                        final Object obj = Array.get(val, i);
                        if ( typePostFix == null ) {
                            if ( obj instanceof String ) {
                                w.write((String)obj);
                            } else if ( obj instanceof Boolean ) {
                                w.write((Boolean)obj);
                            } else if ( obj instanceof Long ) {
                                w.write((Long)obj);
                            } else if ( obj instanceof Double ) {
                                w.write((Double)obj);
                            }
                        } else {
                            w.write(obj.toString());
                        }
                    }
                    w.writeEnd();
                } else {
                    if ( typePostFix == null ) {
                        if ( val instanceof String ) {
                            w.write(name, (String)val);
                        } else if ( val instanceof Boolean ) {
                            w.write(name, (Boolean)val);
                        } else if ( val instanceof Long ) {
                            w.write(name, (Long)val);
                        } else if ( val instanceof Double ) {
                            w.write(name, (Double)val);
                        }
                    } else {
                        w.write(name + typePostFix, val.toString());
                    }
                }
            }

            w.writeEnd();
        }
    }

    protected void writeFrameworkProperties(final JsonGenerator w, final KeyValueMap props) {
        // framework properties
        if ( !props.isEmpty() ) {
            w.writeStartObject(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES);

            for(final Map.Entry<String, String> entry : props) {
                w.write(entry.getKey(), entry.getValue());
            }
            w.writeEnd();
        }
    }

    protected void writeExtensions(final JsonGenerator w,
            final List<Extension> extensions,
            final Configurations allConfigs) {
        for(final Extension ext : extensions) {
            final String key = ext.getName() + ":" + ext.getType().name() + "|" + ext.isOptional();
            if ( ext.getType() == ExtensionType.JSON ) {
                final JsonStructure struct;
                try ( final StringReader reader = new StringReader(ext.getJSON()) ) {
                    struct = Json.createReader(reader).read();
                }
                w.write(key, struct);
            } else if ( ext.getType() == ExtensionType.TEXT ) {
                w.write(key, ext.getText());
            } else {
                w.writeStartArray(key);
                for(final Artifact artifact : ext.getArtifacts()) {
                    final Configurations artifactCfgs = new Configurations();
                    for(final Configuration cfg : allConfigs) {
                        final String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT);
                        if (  artifact.getId().toMvnId().equals(artifactProp) ) {
                            artifactCfgs.add(cfg);
                        }
                    }
                    if ( artifact.getMetadata().isEmpty() && artifactCfgs.isEmpty() ) {
                        w.write(artifact.getId().toMvnId());
                    } else {
                        w.writeStartObject();
                        w.write(JSONConstants.ARTIFACT_ID, artifact.getId().toMvnId());

                        for(final Map.Entry<String, String> me : artifact.getMetadata()) {
                            w.write(me.getKey(), me.getValue());
                        }

                        writeConfigurations(w, artifactCfgs);
                        w.writeEnd();
                    }
                }
                w.writeEnd();
            }
        }
    }
}
