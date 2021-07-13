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
public class ScaleHandler implements TransformationHandler {

    public static final String RESOURCE_TYPE = "sling/thumbnails/transformers/scale";
    public static final String PN_BOTH = "both";

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream, TransformationHandlerConfig config)
            throws IOException {
        ValueMap properties = config.getProperties();
        double both = properties.get(PN_BOTH, -1.0);
        double width = properties.get(ResizeHandler.PN_WIDTH, -1.0);
        double height = properties.get(ResizeHandler.PN_HEIGHT, -1.0);
        try {
            Builder<? extends InputStream> builder = Thumbnails.of(inputStream);
            if (both >= 0) {
                builder.scale(both);
            } else if (width >= 0 && height >= 0) {
                builder.scale(width, height);
            } else {
                throw new BadRequestException("Could not scale thumbnail, invalid parameters: \n%s", properties);
            }

            builder.toOutputStream(outputStream);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unable to resize due to invalid configuration: \n%s", config.getProperties(),
                    e);
        }
    }

}
