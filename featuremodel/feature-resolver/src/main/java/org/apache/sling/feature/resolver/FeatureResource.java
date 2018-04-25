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
package org.apache.sling.feature.resolver;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Feature;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

/**
 * A Resource that is associated with an Maven Artifact and belongs to a Feature.
 */
public interface FeatureResource extends Resource {
    /**
     * Obtain the ID of the resource. If the resource is a bundle then this
     * is the bundle symbolic name.
     * @return The ID of the resource.
     */
    String getId();

    /**
     * Obtain the version of the resource.
     * @return The version of the resource.
     */
    Version getVersion();

    /**
     * Obtain the associated (Maven) Artifact.
     * @return The artifact for this Resource.
     */
    Artifact getArtifact();

    /**
     * Obtain the feature that contains this resource.
     * @return The feature that contains the resource.
     */
    Feature getFeature();
}
