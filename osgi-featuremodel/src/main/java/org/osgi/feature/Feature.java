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

import org.osgi.framework.Version;

public class Feature {
    private final String groupId;
    private final String artifactId;
    private final Version version;
    private final String description;

    private Feature(String gid, String aid, Version ver, String desc) {
        groupId = gid;
        artifactId = aid;
        version = ver;
        description = desc;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Version getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public class Builder {
        private final String groupId;
        private final String artifactId;
        private final Version version;

        private String description;

        public Builder(String groupId, String artifactId, Version version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Feature build() {
            return new Feature(groupId, artifactId, version,
                    description);
        }
    }
}
