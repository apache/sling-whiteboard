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
package org.apache.sling.thumbnails.internal.providers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.common.net.MediaType;

import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.apache.sling.thumbnails.OutputFileFormat;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides Thumbnails for Microsoft PPT and PPTX files.
 */
@Component(service = ThumbnailProvider.class, immediate = true)
public class SlideShowThumbnailProvider implements ThumbnailProvider {

    private final DynamicClassLoaderManager classLoaderManager;
    private final ThumbnailSupport thumbnailSupport;

    @Activate
    public SlideShowThumbnailProvider(@Reference DynamicClassLoaderManager classLoaderManager,
            @Reference ThumbnailSupport thumbnailSupport) {
        this.classLoaderManager = classLoaderManager;
        this.thumbnailSupport = thumbnailSupport;
    }

    @Override
    public boolean applies(Resource resource, String metaType) {
        MediaType mt = MediaType.parse(metaType);
        return mt.is(MediaType.MICROSOFT_POWERPOINT) || mt.is(MediaType.OOXML_PRESENTATION);
    }

    @Override
    public InputStream getThumbnail(Resource resource) throws IOException {
        if (classLoaderManager != null) {
            Thread.currentThread().setContextClassLoader(classLoaderManager.getDynamicClassLoader());
        }

        SlideShow<?, ?> ppt = null;
        MediaType mt = MediaType.parse(resource.getValueMap()
                .get(thumbnailSupport.getMetaTypePropertyPath(resource.getResourceType()), String.class));
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = resource.adaptTo(InputStream.class)) {
            if (mt.is(MediaType.MICROSOFT_POWERPOINT)) {
                ppt = new HSLFSlideShow(is);
            } else {
                ppt = new XMLSlideShow(is);
            }
            Dimension dim = ppt.getPageSize();
            List<? extends Slide<?, ?>> slides = ppt.getSlides();

            BufferedImage img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = img.createGraphics();
            graphics.setPaint(Color.white);
            graphics.fill(new Rectangle2D.Float(0, 0, dim.width, dim.height));

            if (slides != null && !slides.isEmpty()) {
                slides.get(0).draw(graphics);
            }

            ImageIO.write(img, OutputFileFormat.PNG.toString(), baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } finally {
            if (ppt != null) {
                ppt.close();
            }
        }
    }

}
