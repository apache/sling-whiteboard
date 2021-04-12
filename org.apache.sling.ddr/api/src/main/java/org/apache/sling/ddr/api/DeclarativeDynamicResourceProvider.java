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
package org.apache.sling.ddr.api;

import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.Bundle;

import java.util.List;
import java.util.Map;

/**
 * Declarative Dynamic Resource Provier interface
 */
public interface DeclarativeDynamicResourceProvider {

    /**
     * Register resources in a Provider Folder as the
     * source of Resources in a given Target
     * @param bundle The Bundle where the Resource Provider is registered with
     * @param targetRootPath The Parent Path of the Dynamic Resources
     * @param providerRootPath The Source Folder of the Resources that provide the data
     * @param resourceResolver Resource Resolver to create new Synthetic Resources
     * @return
     */
    long registerService(
        Bundle bundle, String targetRootPath, String providerRootPath, ResourceResolver resourceResolver,
        DeclarativeDynamicResourceManager declarativeDynamicResourceManager,
        Map<String, List<String>> allowedDDRFilter, Map<String, List<String>> prohibitedDDRFilter, List<String> followedLinkNames
    );

    /** Remove the Registration of this Service **/
    void unregisterService();

    /** @return True if this service is active / registered **/
    boolean isActive();

    /** @return The Parent Path of the Declarative Dynamic Resources are served from **/
    String getProviderRootPath();

    /** @return The Parent Path of the Declarative Dynamic Resources are bound too **/
    String getTargetRootPath();

    void update(String path);

}
