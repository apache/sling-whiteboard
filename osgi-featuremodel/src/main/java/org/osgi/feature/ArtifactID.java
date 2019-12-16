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

public class ArtifactID {
    private final String groupId;
    private final String artifactId;
    private final String version; // The Artifact Version may not follow OSGi version rules
    private final String type;
    private final String classifier;

    public static ArtifactID fromMavenID(String mavenID) {
        String[] parts = mavenID.split(":");

        if (parts.length < 3 && parts.length > 5)
            throw new IllegalArgumentException("Not a valid maven ID" + mavenID);

        String gid = parts[0];
        String aid = parts[1];
        String ver = parts[2];
        String t = parts.length > 3 ? parts[3] : null;
        String c = parts.length > 4 ? parts[4] : null;

        return new ArtifactID(gid, aid, ver, t, c);
    }

    public ArtifactID(String groupId, String artifactId, String version, String type, String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.classifier = classifier;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public String getClassifier() {
        return classifier;
    }
}
