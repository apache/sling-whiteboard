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
package org.apache.sling.commons.thumbnails;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A Sling Model interface for retrieving the transformations available to a
 * particular resource.
 * 
 * Assumes that the transformations will be available as context aware
 * configurations under the subpath files/transformations
 */
@ProviderType
public interface TransformationManager {

    /**
     * Gets the transformations available to a particular resource based on the CA
     * Configs
     * 
     * @return a list of transformations
     */
    List<Transformation> getTransformations();
}
