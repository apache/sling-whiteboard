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
package org.apache.sling.feature.support.json;

import org.apache.felix.configurator.impl.json.JSMin;
import org.apache.felix.configurator.impl.json.JSONUtil;
import org.apache.felix.configurator.impl.json.TypeConverter;
import org.apache.felix.configurator.impl.model.Config;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.KeyValueMap;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonWriter;

/**
 * Common methods for JSON reading.
 */
abstract class JSONReaderBase {

    /** The optional location. */
    protected final String location;

    /** Exception prefix containing the location (if set) */
    protected final String exceptionPrefix;

    /**
     * Private constructor
     * @param location Optional location
     */
    JSONReaderBase(final String location) {
        this.location = location;
        if ( location == null ) {
            exceptionPrefix = "";
        } else {
            exceptionPrefix = location + " : ";
        }
    }

    protected String minify(final Reader reader) throws IOException {
       // minify JSON (remove comments)
        final String contents;
        try ( final Writer out = new StringWriter()) {
            final JSMin min = new JSMin(reader, out);
            min.jsmin();
            contents = out.toString();
        }
        return contents;
    }

    /** Get the JSON object as a map, removing all comments that start with a '#' character
     */
    protected Map<String, Object> getJsonMap(JsonObject json) {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) JSONUtil.getValue(json);

        removeComments(m);
        return m;
    }

    private void removeComments(Map<String, Object> m) {
        for(Iterator<Map.Entry<String, Object>> it = m.entrySet().iterator(); it.hasNext(); ) {
            Entry<String, ?> entry = it.next();
            if (entry.getKey().startsWith("#")) {
                it.remove();
            } else if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> embedded = (Map<String, Object>) entry.getValue();
                removeComments(embedded);
            } else if (entry.getValue() instanceof Collection) {
                Collection<?> embedded = (Collection<?>) entry.getValue();
                removeComments(embedded);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void removeComments(Collection<?> embedded) {
        for (Object el : embedded) {
            if (el instanceof Collection) {
                removeComments((Collection<?>) el);
            } else if (el instanceof Map) {
                removeComments((Map<String, Object>) el);
            }
        }
    }

    protected String getProperty(final Map<String, Object> map, final String key) throws IOException {
        final Object val = map.get(key);
        if ( val != null ) {
            checkType(key, val, String.class);
            return val.toString();
        }
        return null;
    }

    /**
     * Read the bundles / start levels section
     * @param map The map describing the feature
     * @param container The bundles container
     * @param configContainer The configurations container
     * @throws IOException If the json is invalid.
     */
    protected void readBundles(
            final Map<String, Object> map,
            final Bundles container,
            final Configurations configContainer) throws IOException {
        if ( map.containsKey(JSONConstants.FEATURE_BUNDLES)) {
            final Object bundlesObj = map.get(JSONConstants.FEATURE_BUNDLES);
            checkType(JSONConstants.FEATURE_BUNDLES, bundlesObj, List.class);

            final List<Artifact> list = new ArrayList<>();
            readArtifacts(JSONConstants.FEATURE_BUNDLES, "bundle", list, bundlesObj, configContainer);

            for(final Artifact a : list) {
                Artifact sameFound = container.getSame(a.getId());
                if ( sameFound != null) {
                    String str1 = a.getMetadata().get("run-modes");
                    String str2 = sameFound.getMetadata().get("run-modes");

                    if (str1 == null ? str2 == null : str1.equals(str2)) {
                        throw new IOException(exceptionPrefix + "Duplicate bundle " + a.getId().toMvnId());
                    }
                }
                try {
                    // check start order
                    a.getStartOrder();
                } catch ( final IllegalArgumentException nfe) {
                    throw new IOException(exceptionPrefix + "Illegal start order '" + a.getMetadata().get(Artifact.KEY_START_ORDER) + "'");
                }
                container.add(a);
            }
        }
    }

    protected void readArtifacts(final String section,
            final String artifactType,
            final List<Artifact> artifacts,
            final Object listObj,
            final Configurations container)
    throws IOException {
        checkType(section, listObj, List.class);
        @SuppressWarnings("unchecked")
        final List<Object> list = (List<Object>) listObj;
        for(final Object entry : list) {
            final Artifact artifact;
            checkType(artifactType, entry, Map.class, String.class);
            if ( entry instanceof String ) {
                artifact = new Artifact(ArtifactId.parse(handleResolveVars(entry).toString()));
            } else {
                @SuppressWarnings("unchecked")
                final Map<String, Object> bundleObj = (Map<String, Object>) entry;
                if ( !bundleObj.containsKey(JSONConstants.ARTIFACT_ID) ) {
                    throw new IOException(exceptionPrefix + " " + artifactType + " is missing required artifact id");
                }
                checkType(artifactType + " " + JSONConstants.ARTIFACT_ID, bundleObj.get(JSONConstants.ARTIFACT_ID), String.class);
                final ArtifactId id = ArtifactId.parse(handleResolveVars(bundleObj.get(JSONConstants.ARTIFACT_ID)).toString());

                artifact = new Artifact(id);
                for(final Map.Entry<String, Object> metadataEntry : bundleObj.entrySet()) {
                    final String key = metadataEntry.getKey();
                    if ( JSONConstants.ARTIFACT_KNOWN_PROPERTIES.contains(key) ) {
                        continue;
                    }
                    checkType(artifactType + " metadata " + key, metadataEntry.getValue(), String.class, Number.class, Boolean.class);
                    artifact.getMetadata().put(key, metadataEntry.getValue().toString());
                }
                if ( bundleObj.containsKey(JSONConstants.FEATURE_CONFIGURATIONS) ) {
                    checkType(artifactType + " configurations", bundleObj.get(JSONConstants.FEATURE_CONFIGURATIONS), Map.class);
                    List<Configuration> bundleConfigs = addConfigurations(bundleObj, artifact, container);
                    artifact.getMetadata().put(JSONConstants.FEATURE_CONFIGURATIONS, bundleConfigs);
                }
            }
            artifacts.add(artifact);
        }
    }

    /** Substitutes variables that need to be specified before the resolver executes.
     * These are variables in features, artifacts (such as bundles), requirements
     * and capabilities.
     * @param val The value that may contain a variable.
     * @return The value with the variable substitiuted.
     */
    protected Object handleResolveVars(Object val) {
        // No variable substitution at this level, but subclasses can add this in
        return val;
    }

    /** Substitutes variables that need to be substituted at launch time.
     * These are all variables that are not needed by the resolver.
     * @param val The value that may contain a variable.
     * @return The value with the variable substitiuted.
     */
    protected Object handleLaunchVars(Object val) {
        // No variable substitution at this level, but subclasses can add this in
        return val;
    }

    protected List<Configuration> addConfigurations(final Map<String, Object> map,
            final Artifact artifact,
            final Configurations container) throws IOException {
        final JSONUtil.Report report = new JSONUtil.Report();
        @SuppressWarnings("unchecked")
        final List<Config> configs = JSONUtil.readConfigurationsJSON(new TypeConverter(null),
                0, "", (Map<String, ?>)map.get(JSONConstants.FEATURE_CONFIGURATIONS), report);
        if ( !report.errors.isEmpty() || !report.warnings.isEmpty() ) {
            final StringBuilder builder = new StringBuilder(exceptionPrefix);
            builder.append("Errors in configurations:");
            for(final String w : report.warnings) {
                builder.append("\n");
                builder.append(w);
            }
            for(final String e : report.errors) {
                builder.append("\n");
                builder.append(e);
            }
            throw new IOException(builder.toString());
        }

        List<Configuration> newConfigs = new ArrayList<>();
        for(final Config c : configs) {
            final int pos = c.getPid().indexOf('~');
            final Configuration config;
            if ( pos != -1 ) {
                config = new Configuration(c.getPid().substring(0, pos), c.getPid().substring(pos + 1));
            } else {
                config = new Configuration(c.getPid());
            }
            final Enumeration<String> keyEnum = c.getProperties().keys();
            while ( keyEnum.hasMoreElements() ) {
                final String key = keyEnum.nextElement();
                if ( key.startsWith(":configurator:") ) {
                    throw new IOException(exceptionPrefix + "Configuration must not define configurator property " + key);
                }
                final Object val = c.getProperties().get(key);
                config.getProperties().put(key, handleLaunchVars(val));
            }
            if ( config.getProperties().get(Configuration.PROP_ARTIFACT) != null ) {
                throw new IOException(exceptionPrefix + "Configuration must not define property " + Configuration.PROP_ARTIFACT);
            }
            if ( artifact != null ) {
                config.getProperties().put(Configuration.PROP_ARTIFACT, artifact.getId().toMvnId());
            }
            for(final Configuration current : container) {
                if ( current.equals(config) ) {
                    throw new IOException(exceptionPrefix + "Duplicate configuration " + config);
                }
            }
            container.add(config);
            newConfigs.add(config);
        }
        return newConfigs;
    }


    protected void readConfigurations(final Map<String, Object> map,
            final Configurations container) throws IOException {
        if ( map.containsKey(JSONConstants.FEATURE_CONFIGURATIONS) ) {
            checkType(JSONConstants.FEATURE_CONFIGURATIONS, map.get(JSONConstants.FEATURE_CONFIGURATIONS), Map.class);
            addConfigurations(map, null, container);
        }
    }

    protected void readFrameworkProperties(final Map<String, Object> map,
            final KeyValueMap container) throws IOException {
        if ( map.containsKey(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES) ) {
            final Object propsObj= map.get(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES);
            checkType(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES, propsObj, Map.class);

            @SuppressWarnings("unchecked")
            final Map<String, Object> props = (Map<String, Object>) propsObj;
            for(final Map.Entry<String, Object> entry : props.entrySet()) {
                checkType("framework property value", entry.getValue(), String.class, Boolean.class, Number.class);
                if ( container.get(entry.getKey()) != null ) {
                    throw new IOException(this.exceptionPrefix + "Duplicate framework property " + entry.getKey());
                }
                container.put(entry.getKey(), handleLaunchVars(entry.getValue()).toString());
            }

        }
    }

    protected void readExtensions(final Map<String, Object> map,
            final List<String> keywords,
            final Extensions container,
            final Configurations configContainer) throws IOException {
        final Set<String> keySet = new HashSet<>(map.keySet());
        keySet.removeAll(keywords);
        // the remaining keys are considered extensions!
        for(final String key : keySet) {
            final int pos = key.indexOf(':');
            final String postfix = pos == -1 ? null : key.substring(pos + 1);
            final int sep = (postfix == null ? key.indexOf('|') : postfix.indexOf('|'));
            final String name;
            final String type;
            final String optional;
            if ( pos == -1 ) {
                type = ExtensionType.ARTIFACTS.name();
                if ( sep == -1 ) {
                    name = key;
                    optional = Boolean.FALSE.toString();
                } else {
                    name = key.substring(0, sep);
                    optional = key.substring(sep + 1);
                }
            } else {
                name = key.substring(0, pos);
                if ( sep == -1 ) {
                    type = postfix;
                    optional = Boolean.FALSE.toString();
                } else {
                    type = postfix.substring(0, sep);
                    optional = postfix.substring(sep + 1);
                }
            }
            if ( JSONConstants.APP_KNOWN_PROPERTIES.contains(name) ) {
                throw new IOException(this.exceptionPrefix + "Extension is using reserved name : " + name);
            }
            if ( JSONConstants.FEATURE_KNOWN_PROPERTIES.contains(name) ) {
                throw new IOException(this.exceptionPrefix + "Extension is using reserved name : " + name);
            }
            if ( container.getByName(name) != null ) {
                throw new IOException(exceptionPrefix + "Duplicate extension with name " + name);
            }

            final ExtensionType extType = ExtensionType.valueOf(type);
            final boolean opt = Boolean.valueOf(optional).booleanValue();

            final Extension ext = new Extension(extType, name, opt);
            final Object value = map.get(key);
            switch ( extType ) {
                case ARTIFACTS : final List<Artifact> list = new ArrayList<>();
                                 readArtifacts("Extension " + name, "artifact", list, value, configContainer);
                                 for(final Artifact a : list) {
                                     if ( ext.getArtifacts().contains(a) ) {
                                         throw new IOException(exceptionPrefix + "Duplicate artifact in extension " + name + " : " + a.getId().toMvnId());
                                     }
                                     ext.getArtifacts().add(a);
                                 }
                                 break;
                case JSON : checkType("JSON Extension " + name, value, Map.class, List.class);
                            final JsonStructure struct = build(value);
                            try ( final StringWriter w = new StringWriter()) {
                                final JsonWriter jw = Json.createWriter(w);
                                jw.write(struct);
                                w.flush();
                                ext.setJSON(w.toString());
                            }
                            break;
                case TEXT : checkType("Text Extension " + name, value, String.class, List.class);
                            if ( value instanceof String ) {
                                // string
                                ext.setText(value.toString());
                            } else {
                                // list (array of strings)
                                @SuppressWarnings("unchecked")
                                final List<Object> l = (List<Object>)value;
                                final StringBuilder sb = new StringBuilder();
                                for(final Object o : l) {
                                    checkType("Text Extension " + name + ", value " + o, o, String.class);
                                    sb.append(o.toString());
                                    sb.append('\n');
                                }
                                ext.setText(sb.toString());
                            }
                            break;
            }

            container.add(ext);
        }
    }

    private JsonStructure build(final Object value) {
        if ( value instanceof List ) {
            @SuppressWarnings("unchecked")
            final List<Object> list = (List<Object>)value;
            final JsonArrayBuilder builder = Json.createArrayBuilder();
            for(final Object obj : list) {
                if ( obj instanceof String ) {
                    builder.add(obj.toString());
                } else if ( obj instanceof Long ) {
                    builder.add((Long)obj);
                } else if ( obj instanceof Double ) {
                    builder.add((Double)obj);
                } else if (obj instanceof Boolean ) {
                    builder.add((Boolean)obj);
                } else if ( obj instanceof Map ) {
                    builder.add(build(obj));
                } else if ( obj instanceof List ) {
                    builder.add(build(obj));
                }

            }
            return builder.build();
        } else if ( value instanceof Map ) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>)value;
            final JsonObjectBuilder builder = Json.createObjectBuilder();
            for(final Map.Entry<String, Object> entry : map.entrySet()) {
                if ( entry.getValue() instanceof String ) {
                    builder.add(entry.getKey(), entry.getValue().toString());
                } else if ( entry.getValue() instanceof Long ) {
                    builder.add(entry.getKey(), (Long)entry.getValue());
                } else if ( entry.getValue() instanceof Double ) {
                    builder.add(entry.getKey(), (Double)entry.getValue());
                } else if ( entry.getValue() instanceof Boolean ) {
                    builder.add(entry.getKey(), (Boolean)entry.getValue());
                } else if ( entry.getValue() instanceof Map ) {
                    builder.add(entry.getKey(), build(entry.getValue()));
                } else if ( entry.getValue() instanceof List ) {
                    builder.add(entry.getKey(), build(entry.getValue()));
                }
            }
            return builder.build();
        }
        return null;
    }

    /**
     * Check if the value is one of the provided types
     * @param key A key for the error message
     * @param val The value to check
     * @param types The allowed types
     * @throws IOException If the val is not of the specified types
     */
    protected void checkType(final String key, final Object val, Class<?>...types) throws IOException {
        boolean valid = false;
        for(final Class<?> c : types) {
            if ( c.isInstance(val) ) {
                valid = true;
                break;
            }
        }
        if ( !valid ) {
            throw new IOException(this.exceptionPrefix + "Key " + key + " is not one of the allowed types " + Arrays.toString(types) + " : " + val.getClass());
        }
    }
}


