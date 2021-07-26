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
package org.apache.sling.thumbnails;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * A model interface for getting the renditions for a resource.
 */
public interface RenderedResource {

    /**
     * Gets the transformations defined in this resource's CA Configs
     * 
     * @return the transformations available for the resource
     */
    @NotNull
    List<Transformation> getAvailableTransformations();

    /**
     * Get the renditions for the requested resource
     * 
     * @return the renditions for the resource
     */
    @NotNull
    List<Resource> getRenditions();

    /**
     * Get the relative path to the renditions within the resource, e.g.
     * jcr:content/renditions
     * 
     * @return the relative path to the renditions
     */
    @NotNull
    String getRenditionsPath();

    /**
     * Gets all of the supported renditions for this resource, e.g. the union of the
     * transformations defined in this resource's CA Configs without extensions and
     * the existing renditions (with extensions)
     * 
     * @return the list of the supported renditions
     */
    List<String> getSupportedRenditions();
}
