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
package org.apache.sling.cpconverter.maven.mojos;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ContentPackage {

    /**
     * The content package Group Id
     */
    private String groupId = "";

    /**
     * The content package Artifact Id
     */
    private String artifactId = "";

    private String type = "zip";

    // TODO: Classifier should not be set as we have the one from the converter
    private String classifier = "";

    private boolean excludeTransitive;
    
    public void setGroupId(String groupId) {
        this.groupId = groupId == null ? "" : groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId == null ? "" : artifactId;
    }

    public void setExcludeTransitive(boolean excludeTransitive) {
        this.excludeTransitive = excludeTransitive;
    }

    public boolean isExcludeTransitive() {
        return excludeTransitive;
    }
    
    public Collection<Artifact> getMatchingArtifacts(final MavenProject project) {
        // get artifacts depending on whether we exclude transitives or not
        final Set<Artifact> artifacts;
        // TODO: when I ran the tests the artifacts where only available in the Dependency Artifacts and
        //       getArtifacts() returned an empty set
        if (excludeTransitive) {
            // only direct dependencies, transitives excluded
            artifacts = project.getDependencyArtifacts();
        } else {
            // all dependencies, transitives included
            artifacts = project.getArtifacts();
        }
        return getMatchingArtifacts(artifacts);
    }

    public Collection<Artifact> getMatchingArtifacts(final Collection<Artifact> artifacts) {
        final List<Artifact> matches = new ArrayList<Artifact>();
        for (Artifact artifact : artifacts) {
            if(groupId.equals(artifact.getGroupId())
                && artifactId.equals(artifact.getArtifactId())
                && type.equals(artifact.getType())
                && (classifier.equals(artifact.getClassifier()) || (classifier.equals("") && artifact.getClassifier() == null))
            ) {
                matches.add(artifact);
            }
        }
        return matches;
    }

    @Nonnull
    public StringBuilder toString(@Nullable StringBuilder builder) {
        if (builder == null) {
            builder = new StringBuilder();
        }
        builder.append("groupId=").append(groupId).append(",");
        builder.append("artifactId=").append(artifactId).append(",");

        if (type != null) {
            builder.append("type=").append(type).append(",");
        }
        if (classifier != null) {
            builder.append("classifier=").append(classifier).append(",");
        }
        builder.append(",excludeTransitive=").append(excludeTransitive);
        return builder;
    }

    public String toString() {
        return toString(null).toString();
    }
}
