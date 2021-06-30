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
package org.apache.sling.feature.osgi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureExtension.Kind;
import org.osgi.service.feature.FeatureExtension.Type;
import org.osgi.service.feature.FeatureExtensionBuilder;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;

class ExtensionBuilderImpl implements FeatureExtensionBuilder {
	private static FeatureService FEATURE_SERVICE = new FeatureServiceImpl();
	
    private final String name;
    private final Type type;
    private final Kind kind;

    private final List<String> content = new ArrayList<>();

    ExtensionBuilderImpl(String name, Type type, Kind kind) {
        this.name = name;
        this.type = type;
        this.kind = kind;
    }

    @Override
    public FeatureExtensionBuilder addText(String text) {
        if (type != Type.TEXT)
            throw new IllegalStateException("Cannot add text to extension of type " + type);

        content.add(text);
        return this;
    }

    @Override
    public FeatureExtensionBuilder setJSON(String json) {
        if (type != Type.JSON)
            throw new IllegalStateException("Cannot add text to extension of type " + type);

        content.clear(); // Clear any previous value
        content.add(json);
        return this;
    }

    @Override
    public FeatureExtensionBuilder addArtifact(ID id) {
        if (type != Type.ARTIFACTS)
            throw new IllegalStateException("Cannot add artifacts to extension of type " + type);

        StringBuilder aid = new StringBuilder();
        aid.append(id.getGroupId());
        aid.append(':');
        aid.append(id.getArtifactId());
        aid.append(':');
        aid.append(id.getVersion());

        id.getType().ifPresent(
    		t -> {
                aid.append(':');
                aid.append(t);
                
                id.getClassifier().ifPresent(
            		c -> {
                        aid.append(':');
                        aid.append(2);
                		}
            		);
    		});
        aid.append('\n');
        content.add(aid.toString());
        return this;
    }

    @Override
    public FeatureExtensionBuilder addArtifact(String groupId, String artifactId, String version) {
        return addArtifact(groupId, artifactId, version, null, null);
    }

    @Override
    public FeatureExtensionBuilder addArtifact(String groupId, String artifactId, String version, String at, String classifier) {
    	return addArtifact(FEATURE_SERVICE.getID(groupId, artifactId, version, at, classifier)); 
    }
    
    @Override
    public FeatureExtension build() {
        return new ExtensionImpl(name, type, kind, content);
    }

    private static class ExtensionImpl implements FeatureExtension {
        private final String name;
        private final Type type;
        private final Kind kind;
        private final List<String> content;

        private ExtensionImpl(String name, Type type, Kind kind, List<String> content) {
            this.name = name;
            this.type = type;
            this.kind = kind;
            this.content = Collections.unmodifiableList(content);
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public Kind getKind() {
            return kind;
        }

        public String getJSON() {
            if (type != Type.JSON)
                throw new IllegalStateException("Extension is not of type JSON " + type);

            if (content.isEmpty())
                return null;

            return content.get(0);
        }

        public List<String> getText() {
            if (type != Type.TEXT)
                throw new IllegalStateException("Extension is not of type Text " + type);

            return content;
        }

        public List<FeatureArtifact> getArtifacts() {
            List<FeatureArtifact> res = new ArrayList<>();

            for (String s : content) {
            	res.add(FEATURE_SERVICE.getBuilderFactory().newArtifactBuilder(
            			FEATURE_SERVICE.getIDfromMavenID(s)).build());
            }

            return res;
        }

        @Override
        public int hashCode() {
            return Objects.hash(content, name, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof ExtensionImpl))
                return false;
            ExtensionImpl other = (ExtensionImpl) obj;
            return Objects.equals(content, other.content) && Objects.equals(name, other.name) && type == other.type;
        }

        @Override
        public String toString() {
            return "ExtensionImpl [name=" + name + ", type=" + type + "]";
        }
    }
}
