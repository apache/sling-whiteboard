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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.google.common.net.MediaType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.thumbnails.extension.ThumbnailProvider;
import org.osgi.service.component.annotations.Component;

/**
 * A thumbnail provider for PDF documents.
 */
@Component(service = ThumbnailProvider.class, immediate = true)
public class PdfThumbnailProvider implements ThumbnailProvider {

    @Override
    public boolean applies(Resource resource, String metaType) {
        return MediaType.PDF.is(MediaType.parse(metaType));
    }

    @Override
    public InputStream getThumbnail(Resource resource) throws IOException {
        try (PDDocument document = PDDocument.load(resource.adaptTo(InputStream.class))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(bim, "jpeg", os);
            return new ByteArrayInputStream(os.toByteArray());
        }
    }
}
