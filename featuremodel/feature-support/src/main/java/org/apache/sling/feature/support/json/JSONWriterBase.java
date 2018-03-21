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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.KeyValueMap;

import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

/**
 * Common functionality for writing JSON
 */
abstract class JSONWriterBase {
    private final char factoryConfigSeparator;

    protected JSONWriterBase(WriteOption ... opts) {
        if (Arrays.asList(opts).contains(WriteOption.OLD_STYLE_FACTORY_CONFIGS)) {
            factoryConfigSeparator = '-';
        } else {
            factoryConfigSeparator = '~';
        }
    }

    protected void writeBundles(final JsonObjectBuilder ob,
            final Bundles bundles,
            final Configurations allConfigs) {
        // bundles
        if ( !bundles.isEmpty() ) {
            JsonArrayBuilder bundleArray = Json.createArrayBuilder();

            for(final Artifact artifact : bundles) {
                final Configurations cfgs = new Configurations();
                for(final Configuration cfg : allConfigs) {
                    String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT);
                    if (artifactProp != null) {
                        if (artifactProp.startsWith("mvn:")) {
                            // Change Maven URL to maven GAV syntax
                            artifactProp = artifactProp.substring("mvn:".length());
                            artifactProp = artifactProp.replace('/', ':');
                        }
                        if (artifact.getId().toMvnId().equals(artifactProp)) {
                            cfgs.add(cfg);
                        }
                    }
                }
                KeyValueMap md = artifact.getMetadata();
                if ( md.isEmpty() && cfgs.isEmpty() ) {
                    bundleArray.add(artifact.getId().toMvnId());
                } else {
                    JsonObjectBuilder bundleObj = Json.createObjectBuilder();
                    bundleObj.add(JSONConstants.ARTIFACT_ID, artifact.getId().toMvnId());

                    if (md.get("start-level") == null) {
                        String so = md.get("start-order");
                        if (so != null) {
                            md.put("start-level", so);
                        }
                    }

                    Object runmodes = md.remove("runmodes");
                    if (runmodes instanceof String) {
                        md.put("run-modes", runmodes);
                    }

                    for(final Map.Entry<String, String> me : md) {
                        bundleObj.add(me.getKey(), me.getValue());
                    }

                    writeConfigurations(bundleObj, cfgs);

                    bundleArray.add(bundleObj.build());
                }
            }
            ob.add(JSONConstants.FEATURE_BUNDLES, bundleArray.build());
        }
    }

    /**
     * Write the list of configurations into a "configurations" element
     * @param ob The json generator
     * @param cfgs The list of configurations
     */
    protected void writeConfigurations(final JsonObjectBuilder ob, final Configurations cfgs) {
        if ( !cfgs.isEmpty() ) {
            ob.add(JSONConstants.FEATURE_CONFIGURATIONS,
                    writeConfigurationsMap(cfgs));
        }
    }

    /**
     * Write the list of configurations into a "configurations" element
     * @param w The json generator
     * @param cfgs The list of configurations
     * @return
     */
    protected JsonObject writeConfigurationsMap(final Configurations cfgs) {
        JsonObjectBuilder configObj = Json.createObjectBuilder();
        for(final Configuration cfg : cfgs) {
            final String key;
            if ( cfg.isFactoryConfiguration() ) {
                key = cfg.getFactoryPid() + factoryConfigSeparator + cfg.getName();
            } else {
                key = cfg.getPid();
            }
            JsonObjectBuilder cfgValObj = Json.createObjectBuilder();

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
                    JsonArrayBuilder ab = Json.createArrayBuilder();
                    for(int i=0; i<Array.getLength(val);i++ ) {
                        final Object obj = Array.get(val, i);
                        if ( typePostFix == null ) {
                            if ( obj instanceof String ) {
                                ab.add((String)obj);
                            } else if ( obj instanceof Boolean ) {
                                ab.add((Boolean)obj);
                            } else if ( obj instanceof Long ) {
                                ab.add((Long)obj);
                            } else if ( obj instanceof Double ) {
                                ab.add((Double)obj);
                            }
                        } else {
                            ab.add(obj.toString());
                        }
                    }
                    cfgValObj.add(name, ab.build());
                } else {
                    if ( typePostFix == null ) {
                        if ( val instanceof String ) {
                            cfgValObj.add(name, (String)val);
                        } else if ( val instanceof Boolean ) {
                            cfgValObj.add(name, (Boolean)val);
                        } else if ( val instanceof Long ) {
                            cfgValObj.add(name, (Long)val);
                        } else if ( val instanceof Double ) {
                            cfgValObj.add(name, (Double)val);
                        }
                    } else {
                        cfgValObj.add(name + typePostFix, val.toString());
                    }
                }
            }
            configObj.add(key, cfgValObj.build());
        }
        return configObj.build();
    }

    protected void writeVariables(final JsonObjectBuilder ob, final KeyValueMap vars) {
        if ( !vars.isEmpty()) {
            JsonObjectBuilder varsObj = Json.createObjectBuilder();
            for (final Map.Entry<String, String> entry : vars) {
                varsObj.add(entry.getKey(), entry.getValue());
            }
            ob.add(JSONConstants.FEATURE_VARIABLES, varsObj.build());
        }
    }

    protected void writeFrameworkProperties(final JsonObjectBuilder ob, final KeyValueMap props) {
        // framework properties
        if ( !props.isEmpty() ) {
            JsonObjectBuilder propsObj = Json.createObjectBuilder();
            for(final Map.Entry<String, String> entry : props) {
                propsObj.add(entry.getKey(), entry.getValue());
            }
            ob.add(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES, propsObj.build());
        }
    }

    protected void writeExtensions(final JsonObjectBuilder ob,
            final List<Extension> extensions,
            final Configurations allConfigs) {
        for(final Extension ext : extensions) {
            final String key = ext.getName() + ":" + ext.getType().name() + "|" + ext.isOptional();
            if ( ext.getType() == ExtensionType.JSON ) {
                final JsonStructure struct;
                try ( final StringReader reader = new StringReader(ext.getJSON()) ) {
                    struct = Json.createReader(reader).read();
                }
                ob.add(key, struct);
            } else if ( ext.getType() == ExtensionType.TEXT ) {
                ob.add(key, ext.getText());
            } else {
                JsonArrayBuilder extensionArr = Json.createArrayBuilder();
                for(final Artifact artifact : ext.getArtifacts()) {
                    final Configurations artifactCfgs = new Configurations();
                    for(final Configuration cfg : allConfigs) {
                        final String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT);
                        if (  artifact.getId().toMvnId().equals(artifactProp) ) {
                            artifactCfgs.add(cfg);
                        }
                    }
                    if ( artifact.getMetadata().isEmpty() && artifactCfgs.isEmpty() ) {
                        extensionArr.add(artifact.getId().toMvnId());
                    } else {
                        JsonObjectBuilder extObj = Json.createObjectBuilder();
                        extObj.add(JSONConstants.ARTIFACT_ID, artifact.getId().toMvnId());

                        for(final Map.Entry<String, String> me : artifact.getMetadata()) {
                            extObj.add(me.getKey(), me.getValue());
                        }

                        writeConfigurations(ob, artifactCfgs);
                        extensionArr.add(extObj.build());
                    }
                }
                ob.add(key, extensionArr.build());
            }
        }
    }
}
