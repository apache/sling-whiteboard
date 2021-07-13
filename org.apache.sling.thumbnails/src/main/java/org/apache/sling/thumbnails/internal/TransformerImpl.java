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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.apache.sling.thumbnails.extension.TransformationHandler;
import org.apache.sling.thumbnails.BadRequestException;
import org.apache.sling.thumbnails.OutputFileFormat;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.apache.sling.thumbnails.Transformation;
import org.apache.sling.thumbnails.TransformationHandlerConfig;
import org.apache.sling.thumbnails.Transformer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;

@Component(service = Transformer.class)
public class TransformerImpl implements Transformer {

    private static final Logger log = LoggerFactory.getLogger(TransformerImpl.class);

    private final List<TransformationHandler> handlers;

    private final List<ThumbnailProvider> thumbnailProviders;

    private final ThumbnailSupport thumbnailSupport;


    @Activate
    public TransformerImpl(
            @Reference(service = ThumbnailProvider.class, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.AT_LEAST_ONE) List<ThumbnailProvider> thumbnailProviders,
            @Reference ThumbnailSupport thumbnailSupport,
            @Reference(service = TransformationHandler.class, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.AT_LEAST_ONE) List<TransformationHandler> handlers) {
        this.thumbnailProviders = thumbnailProviders;
        this.thumbnailSupport = thumbnailSupport;
        this.handlers = handlers;
    }

    public List<TransformationHandler> getHandlers() {
        return handlers;
    }

    private String getMetaType(Resource resource) {
        return resource.getValueMap().get(thumbnailSupport.getMetaTypePropertyPath(resource.getResourceType()),
                String.class);
    }

    private ThumbnailProvider getThumbnailProvider(Resource resource) throws IOException {
        String metaType = getMetaType(resource);
        log.debug("Finding thumbnail provider for resource {} with meta type {} from available providers {}", resource,
                metaType, thumbnailProviders);
        return thumbnailProviders.stream().filter(tp -> {
            log.debug("Checking provider: {}", tp);
            return tp.applies(resource, metaType);
        }).findFirst()
                .orElseThrow(() -> new IOException("Unable to find thumbnail provider for: " + resource.getPath()));
    }

    /**
     * @return the thumbnailProviders
     */
    public List<ThumbnailProvider> getThumbnailProviders() {
        return thumbnailProviders;
    }

    public TransformationHandler getTransformationHandler(String resourceType) {
        return handlers.stream().filter(h -> resourceType.equals(h.getResourceType())).findFirst().orElse(null);
    }

    @Override
    public void transform(Resource resource, Transformation transformation, OutputFileFormat format, OutputStream out)
            throws IOException {
        if (!thumbnailSupport.getSupportedTypes().contains(resource.getResourceType())) {
            throw new BadRequestException("Unsupported resource type: " + resource.getResourceType());
        }
        ThumbnailProvider provider = getThumbnailProvider(resource);
        log.debug("Using thumbnail provider {} for resource {}", provider, resource);
        try (InputStream thumbnailIs = provider.getThumbnail(resource)) {

            InputStream inputStream = thumbnailIs;
            for (TransformationHandlerConfig config : transformation.getHandlers()) {
                log.debug("Handling command: {}", config);

                TransformationHandler handler = getTransformationHandler(config.getHandlerType());
                if (handler != null) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    log.debug("Invoking handler {} for command {}", handler.getClass().getCanonicalName(),
                            config.getHandlerType());
                    handler.handle(inputStream, outputStream, config);
                    inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                } else {
                    log.info("No handler found for: {}", config.getHandlerType());
                }
            }
            if (!getMetaType(resource).equals(format.getMimeType())) {
                log.debug("Converting to {}", format);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Builder<? extends InputStream> builder = Thumbnails.of(inputStream);
                builder.outputFormat(format.toString());
                builder.scale(1.0);
                builder.toOutputStream(baos);
                inputStream = new ByteArrayInputStream(baos.toByteArray());
            }

            IOUtils.copy(inputStream, out);
        }
    }

}
