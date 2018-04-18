/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.builder;

import java.io.StringReader;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.KeyValueMap;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * Utility methods for the builders
 */
class BuilderUtil {

    public enum ArtifactMerge {
        LATEST,
        HIGHEST
    };

    // bundles
    static void mergeBundles(final Bundles target,
            final Bundles source,
            final ArtifactMerge artifactMergeAlg) {
        for(final Map.Entry<Integer, List<Artifact>> entry : source.getBundlesByStartOrder().entrySet()) {
            for(final Artifact a : entry.getValue()) {
                // version handling - use provided algorithm
                boolean replace = true;
                if ( artifactMergeAlg == ArtifactMerge.HIGHEST ) {
                    final Artifact existing = target.getSame(a.getId());
                    if ( existing != null && existing.getId().getOSGiVersion().compareTo(a.getId().getOSGiVersion()) > 0 ) {
                        replace = false;
                    }
                }
                if ( replace ) {
                    target.removeSame(a.getId());
                    target.add(a);
                }
            }
        }
    }

    // configurations - merge / override
    static void mergeConfigurations(final Configurations target, final Configurations source) {
        for(final Configuration cfg : source) {
            boolean found = false;
            for(final Configuration current : target) {
                if ( current.compareTo(cfg) == 0 ) {
                    found = true;
                    // merge / override properties
                    final Enumeration<String> i = cfg.getProperties().keys();
                    while ( i.hasMoreElements() ) {
                        final String key = i.nextElement();
                        current.getProperties().put(key, cfg.getProperties().get(key));
                    }
                    break;
                }
            }
            if ( !found ) {
                target.add(cfg);
            }
        }
    }

    // framework properties (add/merge)
    static void mergeFrameworkProperties(final KeyValueMap target, final KeyValueMap source) {
        target.putAll(source);
    }

    // requirements (add)
    static void mergeRequirements(final List<Requirement> target, final List<Requirement> source) {
        for(final Requirement req : source) {
            if ( !target.contains(req) ) {
                target.add(req);
            }
        }
    }

    // capabilities (add)
    static void mergeCapabilities(final List<Capability> target, final List<Capability> source) {
        for(final Capability cap : source) {
            if ( !target.contains(cap) ) {
                target.add(cap);
            }
        }
    }

    // default merge for extensions
    static void mergeExtensions(final Extension target,
            final Extension source,
            final ArtifactMerge artifactMergeAlg) {
        switch ( target.getType() ) {
            case TEXT : // simply append
                        target.setText(target.getText() + "\n" + source.getText());
                        break;
            case JSON : final JsonStructure struct1;
                        try ( final StringReader reader = new StringReader(target.getJSON()) ) {
                            struct1 = Json.createReader(reader).read();
                        }
                        final JsonStructure struct2;
                        try ( final StringReader reader = new StringReader(source.getJSON()) ) {
                            struct2 = Json.createReader(reader).read();
                        }

                        if ( struct1.getValueType() != struct2.getValueType() ) {
                            throw new IllegalStateException("Found different JSON types for extension " + target.getName()
                                + " : " + struct1.getValueType() + " and " + struct2.getValueType());
                        }
                        if ( struct1.getValueType() == ValueType.ARRAY ) {
                            // array is append
                            final JsonArray a1 = (JsonArray)struct1;
                            final JsonArray a2 = (JsonArray)struct2;
                            for(final JsonValue val : a2) {
                                a1.add(val);
                            }
                        } else {
                            // object is merge
                            merge((JsonObject)struct1, (JsonObject)struct2);
                        }
                        break;

            case ARTIFACTS : for(final Artifact a : source.getArtifacts()) {
                                 // use artifactMergeAlg
                                 boolean add = true;
                                 for(final Artifact targetArtifact : target.getArtifacts()) {
                                     if ( targetArtifact.getId().isSame(a.getId()) ) {
                                         if ( artifactMergeAlg == ArtifactMerge.HIGHEST ) {
                                             if ( targetArtifact.getId().getOSGiVersion().compareTo(a.getId().getOSGiVersion()) > 0 ) {
                                                 add = false;
                                             } else {
                                                 target.getArtifacts().remove(targetArtifact);
                                             }
                                         } else { // latest

                                             target.getArtifacts().remove(targetArtifact);
                                         }
                                         break;
                                     }
                                 }

                                 if ( add ) {
                                     target.getArtifacts().add(a);
                                 }

                             }
                             break;
        }
    }

    // extensions (add/merge)
    static void mergeExtensions(final Feature target,
            final Feature source,
            final ArtifactMerge artifactMergeAlg,
            final BuilderContext context) {
        for(final Extension ext : source.getExtensions()) {
            boolean found = false;
            for(final Extension current : target.getExtensions()) {
                if ( current.getName().equals(ext.getName()) ) {
                    found = true;
                    if ( current.getType() != ext.getType() ) {
                        throw new IllegalStateException("Found different types for extension " + current.getName()
                        + " : " + current.getType() + " and " + ext.getType());
                    }
                    boolean handled = false;
                    for(final FeatureExtensionHandler fem : context.getFeatureExtensionHandlers()) {
                        if ( fem.canMerge(current.getName()) ) {
                            fem.merge(target, source, current.getName());
                            handled = true;
                            break;
                        }
                    }
                    if ( !handled ) {
                        // default merge
                        mergeExtensions(current, ext, artifactMergeAlg);
                    }
                }
            }
            if ( !found ) {
                target.getExtensions().add(ext);
            }
        }
        // post processing
        for(final Extension ext : target.getExtensions()) {
            for(final FeatureExtensionHandler fem : context.getFeatureExtensionHandlers()) {
                fem.postProcess(target, ext.getName());
            }
        }
    }

    static void mergeExtensions(final Application target,
            final Feature source,
            final ArtifactMerge artifactMergeAlg) {
        for(final Extension ext : source.getExtensions()) {
            boolean found = false;
            for(final Extension current : target.getExtensions()) {
                if ( current.getName().equals(ext.getName()) ) {
                    found = true;
                    if ( current.getType() != ext.getType() ) {
                        throw new IllegalStateException("Found different types for extension " + current.getName()
                        + " : " + current.getType() + " and " + ext.getType());
                    }
                    // default merge
                    mergeExtensions(current, ext, artifactMergeAlg);
                }
            }
            if ( !found ) {
                target.getExtensions().add(ext);
            }
        }
    }

    private static void merge(final JsonObject obj1, final JsonObject obj2) {
        for(final Map.Entry<String, JsonValue> entry : obj2.entrySet()) {
            if ( !obj1.containsKey(entry.getKey()) ) {
                obj1.put(entry.getKey(), entry.getValue());
            } else {
                final JsonValue oldValue = obj1.get(entry.getKey());
                if ( oldValue.getValueType() != entry.getValue().getValueType() ) {
                    // new type wins
                    obj1.put(entry.getKey(), entry.getValue());
                } else if ( oldValue.getValueType() == ValueType.ARRAY ) {
                    final JsonArray a1 = (JsonArray)oldValue;
                    final JsonArray a2 = (JsonArray)entry.getValue();
                    for(final JsonValue val : a2) {
                        a1.add(val);
                    }

                } else if ( oldValue.getValueType() == ValueType.OBJECT ) {
                    merge((JsonObject)oldValue, (JsonObject)entry.getValue());
                } else {
                    obj1.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
