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

import java.awt.Color;
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
import net.coobird.thumbnailator.filters.Colorize;

/**
 * A transformer for resizing an image
 */
@Component(service = TransformationHandler.class, immediate = true)
public class ColorizeHandler implements TransformationHandler {

    public static final String RESOURCE_TYPE = "sling/thumbnails/transformers/colorize";
    public static final String PN_RED = "red";
    public static final String PN_GREEN = "green";
    public static final String PN_BLUE = "blue";
    public static final String PN_ALPHA = "alpha";

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream, TransformationHandlerConfig config)
            throws IOException {
        Builder<? extends InputStream> builder = Thumbnails.of(inputStream);
        ValueMap properties = config.getProperties();
        int red = getColor(properties, PN_RED);
        int green = getColor(properties, PN_GREEN);
        int blue = getColor(properties, PN_BLUE);
        float alpha = (float) config.getProperties().get(PN_ALPHA, 0.0).doubleValue();

        if (alpha < 0 || alpha > 1.0) {
            throw new BadRequestException("Unable to colorize, bad alpha value " + alpha);
        }

        builder.addFilter(new Colorize(new Color(red, green, blue), alpha));
        builder.scale(1.0);
        builder.toOutputStream(outputStream);
    }

    protected int getColor(ValueMap properties, String name) {
        int color = properties.get(name, 0);
        if (color < 0 || color > 255) {
            throw new BadRequestException("Unable to colorize, bad " + name + " value " + color);
        }
        return color;
    }


}
