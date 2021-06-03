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
import org.apache.sling.sitemap.builder.Sitemap;
import org.apache.sling.sitemap.builder.Url;
import org.apache.sling.sitemap.impl.builder.extensions.ExtensionProviderManager;
import org.jetbrains.annotations.NotNull;

import javax.xml.stream.XMLOutputFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class SitemapImpl implements Sitemap, Closeable {

    static final String SITEMAP_NAMESPACE = "http://www.sitemaps.org/schemas/sitemap/0.9";

    protected final Writer out;

    private final ExtensionProviderManager extensionProviderManager;
    private final XMLOutputFactory xmlWriterFactory;
    private boolean closed = false;
    private UrlImpl pendingUrl;

    private int urlCount = 0;

    public SitemapImpl(Writer writer, ExtensionProviderManager extensionProviderManager) throws IOException {
        this(writer, extensionProviderManager, true);
    }

    public SitemapImpl(Writer writer, ExtensionProviderManager extensionProviderManager, boolean writeHeader)
            throws IOException {
        this.extensionProviderManager = extensionProviderManager;
        this.xmlWriterFactory = XMLOutputFactory.newFactory();
        this.out = writer;

        if (writeHeader) {
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.write("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"");

            for (Map.Entry<String, String> entry : extensionProviderManager.getNamespaces().entrySet()) {
                out.write(' ');
                out.write("xmlns:");
                out.write(entry.getValue());
                out.write("=\"");
                out.write(entry.getKey());
                out.write('"');
            }

            out.write('>');
        }
    }

    public int getUrlCount() {
        return urlCount;
    }

    @Override
    public void close() throws IOException {
        try {
            ensureNotClosed();
            writePendingUrl();
            out.write("</urlset>");
            out.flush();
            closed = true;
        } catch (SitemapException ex) {
            unwrapIOException(ex);
        }
    }

    public void flush() throws IOException {
        try {
            ensureNotClosed();
            writePendingUrl();
            out.flush();
        } catch (SitemapException ex) {
            unwrapIOException(ex);
        }
    }

    @Override
    public @NotNull Url addUrl(@NotNull String location) throws SitemapException {
        ensureNotClosed();
        writePendingUrl();
        pendingUrl = new UrlImpl(location, out, xmlWriterFactory, extensionProviderManager);
        urlCount++;
        return pendingUrl;
    }

    protected boolean writePendingUrl() throws SitemapException {
        if (pendingUrl != null) {
            pendingUrl.write();
            pendingUrl = null;
            return true;
        }

        return false;
    }

    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("Sitemap already closed");
        }
    }

    private static void unwrapIOException(Exception ex) throws IOException {
        if (ex.getCause() instanceof IOException) {
            throw (IOException) ex.getCause();
        } else {
            throw new IOException(ex);
        }
    }
}
