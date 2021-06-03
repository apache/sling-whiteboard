/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.sitemap.impl.builder;

import org.apache.sling.sitemap.SitemapException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class SitemapIndexImpl implements Closeable {

    private final XMLStreamWriter out;

    public SitemapIndexImpl(Writer writer) throws IOException {
        try {
            out = XMLOutputFactory.newFactory().createXMLStreamWriter(writer);

            out.writeStartDocument("UTF-8", "1.0");
            out.writeStartElement("sitemapindex");
            out.writeDefaultNamespace(SitemapImpl.SITEMAP_NAMESPACE);
        } catch (XMLStreamException ex) {
            throw new IOException("Failed to open sitemap index", ex);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            out.writeEndElement();
            out.flush();
            out.close();
        } catch (XMLStreamException ex) {
            throw new IOException("Failed to close sitemap index", ex);
        }
    }

    public void addSitemap(@NotNull String location) throws SitemapException {
        addSitemap(location, null);
    }

    public void addSitemap(@NotNull String location, @Nullable Instant lastModified) throws SitemapException {
        try {
            out.writeStartElement("sitemap");
            out.writeStartElement("loc");
            out.writeCharacters(location);
            out.writeEndElement();
            if (lastModified != null) {
                out.writeStartElement("lastmod");
                out.writeCharacters(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(lastModified.atOffset(ZoneOffset.UTC)));
                out.writeEndElement();
            }
            out.writeEndElement();
        } catch (XMLStreamException ex) {
            throw new SitemapException("Failed to add sitemap to index", ex);
        }
    }
}
