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
package org.apache.sling.feature.analyser.task.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.ArtifactDescriptor;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.support.util.CapabilityMatcher;
import org.osgi.resource.Requirement;

public class CheckRequirementsCapabilities implements AnalyserTask {
    private final String format = "Artifact %s:%s requires %s in start level %d but %s";

    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {
        final SortedMap<Integer, List<ArtifactDescriptor>> artifactsMap = new TreeMap<>();
        for(final BundleDescriptor bi : ctx.getDescriptor().getBundleDescriptors()) {
            List<ArtifactDescriptor> list = artifactsMap.get(bi.getBundleStartLevel());
            if ( list == null ) {
                list = new ArrayList<>();
                artifactsMap.put(bi.getBundleStartLevel(), list);
            }
            list.add(bi);
        }

        if (!ctx.getDescriptor().getArtifactDescriptors().isEmpty()) {
            artifactsMap.put(
                    (artifactsMap.isEmpty() ? 0 : artifactsMap.lastKey()) + 1,
                    new ArrayList<>(ctx.getDescriptor().getArtifactDescriptors())
                    );
        }

        // add system artifact
        final List<ArtifactDescriptor> artifacts = new ArrayList<>();
        artifacts.add(ctx.getDescriptor().getFrameworkDescriptor());

        for(final Map.Entry<Integer, List<ArtifactDescriptor>> entry : artifactsMap.entrySet()) {
            // first add all providing artifacts
            for (final ArtifactDescriptor info : entry.getValue()) {
                if (info.getCapabilities() != null) {
                    artifacts.add(info);
                }
            }
            // check requiring artifacts
            for (final ArtifactDescriptor info : entry.getValue()) {
                if (info.getRequirements() != null)
                {
                    for (Requirement requirement : info.getRequirements()) {
                        List<ArtifactDescriptor> candidates = getCandidates(artifacts, requirement);

                        if (candidates.isEmpty()) {
                            if ( "osgi.service".equals(requirement.getNamespace())  ){
                                // osgi.service is special - we don't provide errors or warnings in this case
                                continue;
                            }
                            if (!CapabilityMatcher.isOptional(requirement)) {
                                ctx.reportError(String.format(format, info.getArtifact().getId().getArtifactId(), info.getArtifact().getId().getVersion(), requirement.toString(), entry.getKey(), "no artifact is providing a matching capability in this start level."));
                            }
                            else {
                                ctx.reportWarning(String.format(format, info.getArtifact().getId().getArtifactId(), info.getArtifact().getId().getVersion(), requirement.toString(), entry.getKey(), "while the requirement is optional no artifact is providing a matching capability in this start level."));
                            }
                        }
                        else if ( candidates.size() > 1 ) {
                            ctx.reportWarning(String.format(format, info.getArtifact().getId().getArtifactId(), info.getArtifact().getId().getVersion(), requirement.toString(), entry.getKey(), "there is more than one matching capability in this start level."));
                        }
                    }
                }
            }
        }
    }

    private List<ArtifactDescriptor> getCandidates(List<ArtifactDescriptor> artifactDescriptors, Requirement requirement) {
        return artifactDescriptors.stream()
                .filter(artifactDescriptor -> artifactDescriptor.getCapabilities() != null)
                .filter(artifactDescriptor -> artifactDescriptor.getCapabilities().stream().anyMatch(capability -> CapabilityMatcher.matches(capability, requirement)))
                .collect(Collectors.toList());
    }
}
