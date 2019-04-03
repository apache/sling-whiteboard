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
package org.apache.sling.feature.diff;

import java.util.Objects;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Artifacts;

final class ArtifactsComparator extends AbstractFeatureElementComparator<Artifact, Artifacts> {

    public ArtifactsComparator(String id) {
        super(id);
    }

    @Override
    public String getId(Artifact artifact) {
        return artifact.getId().toMvnId();
    }

    @Override
    public Artifact find(Artifact artifact, Artifacts artifacts) {
        return artifacts.getSame(artifact.getId());
    }

    @Override
    public DiffSection compare(Artifact previous, Artifact current) {
        DiffSection diffSection = new DiffSection(getId(current));

        String previousVersion = previous.getId().getVersion();
        String currentVersion = current.getId().getVersion();
        if (!Objects.equals(previousVersion, currentVersion)) {
            diffSection.markItemUpdated("version", previousVersion, currentVersion);
        }

        if (previous.getStartOrder() != current.getStartOrder()) {
            diffSection.markItemUpdated("start-order", previous.getStartOrder(), current.getStartOrder());
        }

        return diffSection;
    }

}
