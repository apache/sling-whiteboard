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

import org.apache.sling.thumbnails.extension.TransformationHandler;
import org.apache.sling.thumbnails.BadRequestException;
import org.apache.sling.thumbnails.TransformationHandlerConfig;
import org.osgi.service.component.annotations.Component;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;
import net.coobird.thumbnailator.filters.Flip;
import net.coobird.thumbnailator.filters.ImageFilter;

/**
 * Fips the image
 */
@Component(service = TransformationHandler.class, immediate = true)
public class FlipHandler implements TransformationHandler {

    public static final String RESOURCE_TYPE = "sling/thumbnails/transformers/flip";

    public static final String PN_DIRECTION = "direction";

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream, TransformationHandlerConfig config)
            throws IOException {

        String direction = config.getProperties().get(PN_DIRECTION, "").toUpperCase();

        ImageFilter flipper = null;
        if ("HORIZONTAL".equals(direction)) {
            flipper = Flip.HORIZONTAL;
        } else if ("VERTICAL".equals(direction)) {
            flipper = Flip.VERTICAL;
        } else {
            throw new BadRequestException("Could not flip image with configuration: \n%s", config.getProperties());
        }

        Builder<? extends InputStream> builder = Thumbnails.of(inputStream);
        builder.addFilter(flipper);
        builder.scale(1.0);
        builder.toOutputStream(outputStream);
    }

}
