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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Bundle extends ArtifactID {
    private final Map<String, String> metadata;

    private Bundle(String groupId, String artifactId, String version, Map<String,String> metadata) {
        super(groupId, artifactId, version, null, null);

        this.metadata = Collections.unmodifiableMap(metadata);
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public class Builder {
        private final String groupId;
        private final String artifactId;
        private final String version;

        private final Map<String,String> metadata = new HashMap<>();

        public Builder(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public Builder addMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder addMetadata(Map<String,String> md) {
            this.metadata.putAll(md);
            return this;
        }

        public Bundle build() {
            return new Bundle(groupId, artifactId, version, metadata);
        }
    }
}
