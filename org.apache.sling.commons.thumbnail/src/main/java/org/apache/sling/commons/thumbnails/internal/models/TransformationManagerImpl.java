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
package org.apache.sling.commons.thumbnails.internal.models;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.commons.thumbnails.Transformation;
import org.apache.sling.commons.thumbnails.TransformationManager;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

@Model(adaptables = Resource.class, adapters = TransformationManager.class)
public class TransformationManagerImpl implements TransformationManager {

    private final Resource resource;

    private final ConfigurationResourceResolver configResourceResolver;

    @Inject
    public TransformationManagerImpl(@Self Resource resource,
            @OSGiService ConfigurationResourceResolver configResourceResolver) {
        this.resource = resource;
        this.configResourceResolver = configResourceResolver;
    }

    @Override
    public List<Transformation> getTransformations() {
        Collection<Resource> transformationResources = configResourceResolver.getResourceCollection(resource, "files",
                "transformations");
        return transformationResources.stream().map(r -> r.adaptTo(Transformation.class)).collect(Collectors.toList());
    }

}
