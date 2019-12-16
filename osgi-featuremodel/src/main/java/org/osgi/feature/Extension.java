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
package org.osgi.feature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class Extension {
    public enum Type { JSON, TEXT, ARTIFACTS };

    private final String name;
    private final Type type;
    private final String content;

    private Extension(String name, Type type, String content) {
        this.name = name;
        this.type = type;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }


    public String getJSON() {
        if (type != Type.JSON)
            throw new IllegalStateException("Extension is not of type JSON " + type);

        return content;
    }

    public String getText() {
        if (type != Type.TEXT)
            throw new IllegalStateException("Extension is not of type Text " + type);

        return content;
    }

    public List<ArtifactID> getArtifacts() {
        BufferedReader r = new BufferedReader(new StringReader(content));

        List<ArtifactID> res = new ArrayList<>();
        String line = null;
        try {
            while ((line = r.readLine()) != null) {
                res.add(ArtifactID.fromMavenID(line));
            }
        } catch (IOException e) {
            // ignore
        }

        return res;
    }

    public class Builder {
        private final String name;
        private final Type type;

        private final StringBuilder content = new StringBuilder();

        public Builder(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public Builder addText(String text) {
            if (type != Type.TEXT)
                throw new IllegalStateException("Cannot add text to extension of type " + type);

            content.append(text);
            return this;
        }

        public Builder addJSON(String json) {
            if (type != Type.JSON)
                throw new IllegalStateException("Cannot add text to extension of type " + type);

            content.append(json);
            return this;
        }

        public Builder addArtifact(ArtifactID aid) {
            addArtifact(aid.getGroupId(), aid.getArtifactId(), aid.getVersion(), aid.getType(), aid.getClassifier());
            return this;
        }

        public Builder addArtifact(String groupId, String artifactId, String version) {
            if (type != Type.ARTIFACTS)
                throw new IllegalStateException("Cannot add artifacts to extension of type " + type);

            content.append(groupId);
            content.append(':');
            content.append(artifactId);
            content.append(':');
            content.append(version);
            content.append('\n');
            return this;
        }

        public Builder addArtifact(String groupId, String artifactId, String version, String at, String classifier) {
            if (type != Type.ARTIFACTS)
                throw new IllegalStateException("Cannot add artifacts to extension of type " + type);

            content.append(groupId);
            content.append(':');
            content.append(artifactId);
            content.append(':');
            content.append(version);
            content.append(':');
            content.append(at);
            content.append(':');
            content.append(classifier);
            content.append('\n');
            return this;
        }

        public Extension build() {
            return new Extension(name, type, content.toString());
        }
    }
}
