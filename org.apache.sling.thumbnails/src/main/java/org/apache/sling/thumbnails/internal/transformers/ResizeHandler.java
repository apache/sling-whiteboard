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
package org.apache.sling.thumbnails.internal.transformers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.thumbnails.extension.TransformationHandler;
import org.apache.sling.thumbnails.BadRequestException;
import org.apache.sling.thumbnails.TransformationHandlerConfig;
import org.osgi.service.component.annotations.Component;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;

/**
 * A transformer for resizing an image
 */
@Component(service = TransformationHandler.class, immediate = true)
public class ResizeHandler implements TransformationHandler {

    public static final String RESOURCE_TYPE = "sling/thumbnails/transformers/resize";
    public static final String PN_HEIGHT = "height";
    public static final String PN_WIDTH = "width";
    public static final String PN_KEEP_ASPECT_RATIO = "keepAspectRatio";

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream, TransformationHandlerConfig config)
            throws IOException {
        Builder<? extends InputStream> builder = Thumbnails.of(inputStream);

        try {
            resize(builder, config.getProperties());

            boolean keepAspectRatio = config.getProperties().get(PN_KEEP_ASPECT_RATIO, true);
            builder.keepAspectRatio(keepAspectRatio);

            builder.toOutputStream(outputStream);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unable to resize due to invalid configuration: \n%s", config.getProperties(),
                    e);
        }
    }

    private static void resize(Builder<? extends InputStream> builder, ValueMap properties) {
        int width = properties.get(PN_WIDTH, -1);
        int height = properties.get(PN_HEIGHT, -1);
        if (width >= 0 && height >= 0) {
            builder.size(width, height);
        } else if (width >= 0) {
            builder.width(width);
        } else if (height >= 0) {
            builder.height(height);
        } else {
            throw new BadRequestException("Unable to resize thumbnail due to invalid parameters: \n%s", properties);
        }
    }

}
