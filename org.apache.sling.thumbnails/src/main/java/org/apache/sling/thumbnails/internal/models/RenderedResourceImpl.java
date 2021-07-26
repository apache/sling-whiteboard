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
package org.apache.sling.thumbnails.internal.models;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.thumbnails.RenderedResource;
import org.apache.sling.thumbnails.RenditionSupport;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.apache.sling.thumbnails.Transformation;
import org.jetbrains.annotations.NotNull;

@Model(adaptables = { SlingHttpServletRequest.class }, adapters = RenderedResource.class)
public class RenderedResourceImpl implements RenderedResource {

    private final List<Transformation> availableTransformations;
    private final List<Resource> renditions;
    private final String renditionsPath;
    private final List<String> supportedRenditions;

    @Inject
    public RenderedResourceImpl(@Self SlingHttpServletRequest request,
            @OSGiService ConfigurationResourceResolver configResourceResolver,
            @OSGiService RenditionSupport renditionSupport, @OSGiService ThumbnailSupport thumbnailSupport) {
        Resource resource = Optional.ofNullable(request.getResourceResolver().getResource(request.getParameter("src")))
                .orElse(request.getRequestPathInfo().getSuffixResource());

        Resource contextResource = request.getRequestPathInfo().getSuffixResource();

        if (thumbnailSupport.getPersistableTypes().contains(resource.getResourceType())) {
            this.renditions = renditionSupport.listRenditions(resource);
            this.renditionsPath = thumbnailSupport.getRenditionPath(resource.getResourceType());
        } else {
            this.renditions = Collections.emptyList();
            this.renditionsPath = null;
        }

        Collection<Resource> transformationResources = configResourceResolver.getResourceCollection(contextResource,
                "files", "transformations");
        availableTransformations = transformationResources.stream().map(r -> r.adaptTo(Transformation.class))
                .collect(Collectors.toList());
        supportedRenditions = availableTransformations.stream().map(Transformation::getName)
                .collect(Collectors.toList());
        renditions.stream().filter(r -> !supportedRenditions.contains(StringUtils.substringBefore(r.getName(), ".")))
                .map(Resource::getName).forEach(supportedRenditions::add);
    }

    @Override
    public @NotNull List<Transformation> getAvailableTransformations() {
        return this.availableTransformations;
    }

    @Override
    public List<Resource> getRenditions() {
        return this.renditions;
    }

    @Override
    public String getRenditionsPath() {
        return this.renditionsPath;
    }

    @Override
    public List<String> getSupportedRenditions() {
        return supportedRenditions;
    }

}
