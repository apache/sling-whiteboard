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
package org.apache.sling.feature.resolver.impl;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

abstract class AbstractResourceImpl {
    public List<Requirement> getRequirements(String namespace, Map<String, List<Requirement>> requirements) {
        if (namespace == null) {
            return requirements.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        }

        List<Requirement> reqs = requirements.get(namespace);
        if (reqs == null)
            return Collections.emptyList();
        return reqs;
    }

    public List<Capability> getCapabilities(String namespace, Map<String, List<Capability>> capabilities) {
        if (namespace == null) {
            return capabilities.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        }

        List<Capability> caps = capabilities.get(namespace);
        if (caps == null)
            return Collections.emptyList();
        return caps;
    }
}
