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
package org.apache.sling.thumbnails.internal;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.thumbnails.RenditionSupport;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = RenditionSupport.class)
public class RenditionSupportImpl implements RenditionSupport {

    private final ThumbnailSupport thumbnailSupport;
    private final TransformationServiceUser transformationServiceUser;

    @Activate
    public RenditionSupportImpl(@Reference ThumbnailSupport thumbnailSupport,
            @Reference TransformationServiceUser transformationServiceUser) {
        this.thumbnailSupport = thumbnailSupport;
        this.transformationServiceUser = transformationServiceUser;
    }

    @Override
    public @Nullable Resource getRendition(@NotNull Resource file, @NotNull String renditionName) {
        if (supportsRenditions(file)) {
            String subpath = thumbnailSupport.getRenditionPath(file.getResourceType());
            return file.getChild(subpath + "/" + renditionName);
        }
        return null;
    }

    @Override
    public @Nullable InputStream getRenditionContent(@NotNull Resource file, @NotNull String renditionName) {
        return Optional.ofNullable(getRendition(file, renditionName)).map(r -> r.adaptTo(InputStream.class))
                .orElse(null);
    }

    @Override
    public @NotNull List<Resource> listRenditions(@NotNull Resource file) {
        List<Resource> renditions = new ArrayList<>();
        if (this.supportsRenditions(file)) {
            Optional.ofNullable(file.getChild(thumbnailSupport.getRenditionPath(file.getResourceType())))
                    .ifPresent(renditionFolder -> {
                        StreamSupport.stream(renditionFolder.getChildren().spliterator(), false)
                                .filter(c -> JcrConstants.NT_FILE.equals(c.getResourceType())).forEach(renditions::add);
                    });
        }
        return renditions;
    }

    @Override
    public boolean renditionExists(@NotNull Resource file, @NotNull String renditionName) {
        return getRendition(file, renditionName) != null;
    }

    @Override
    public boolean supportsRenditions(@NotNull Resource file) {
        return thumbnailSupport.getPersistableTypes().contains(file.getResourceType());
    }

    @Override
    public void setRendition(@NotNull Resource file, @NotNull String renditionName, @NotNull InputStream contents)
            throws PersistenceException {
        if (renditionName.indexOf("/") != 0) {
            renditionName = "/" + renditionName;
        }
        try (ResourceResolver serviceResolver = transformationServiceUser.getTransformationServiceUser()) {

            Resource renditionFile = ResourceUtil.getOrCreateResource(serviceResolver,
                    file.getPath() + "/" + thumbnailSupport.getRenditionPath(file.getResourceType()) + renditionName,
                    Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE),
                    JcrConstants.NT_UNSTRUCTURED, false);
            Map<String, Object> properties = new HashMap<>();
            properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
            properties.put(JcrConstants.JCR_DATA, contents);
            ResourceUtil.getOrCreateResource(serviceResolver, renditionFile.getPath() + "/" + JcrConstants.JCR_CONTENT,
                    properties, JcrConstants.NT_UNSTRUCTURED, true);
        } catch (LoginException le) {
            throw new PersistenceException("Could not save due to LoginException", le);
        }

    }

}
