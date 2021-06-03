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
import org.apache.sling.sitemap.builder.Extension;
import org.apache.sling.sitemap.builder.Url;
import org.apache.sling.sitemap.builder.extensions.AbstractExtension;
import org.apache.sling.sitemap.impl.builder.extensions.ExtensionFactory;
import org.apache.sling.sitemap.impl.builder.extensions.ExtensionProviderManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class UrlImpl implements Url {

    private static final Logger LOG = LoggerFactory.getLogger(UrlImpl.class);

    private final String location;
    private final Writer out;
    private final XMLOutputFactory xmlWriterFactory;
    private final ExtensionProviderManager extensionProviderManager;

    private boolean written = false;
    private ChangeFrequency changeFrequency;
    private Instant lastModified;
    private Double priority;
    private List<ExtensionMeta> extensions;

    UrlImpl(String location, Writer out, XMLOutputFactory xmlWriterFactory, ExtensionProviderManager extensionProviderManager) {
        this.location = location;
        this.out = out;
        this.xmlWriterFactory = xmlWriterFactory;
        this.extensionProviderManager = extensionProviderManager;
    }

    @Override
    public @NotNull Url setChangeFrequency(@NotNull ChangeFrequency changeFrequency) {
        ensureNotWritten();
        this.changeFrequency = changeFrequency;
        return this;
    }

    @Override
    public @NotNull Url setLastModified(@NotNull Instant pointInTime) {
        ensureNotWritten();
        this.lastModified = pointInTime;
        return this;
    }

    @Override
    public @NotNull Url setPriority(double priority) {
        ensureNotWritten();
        this.priority = priority;
        return this;
    }

    @Override
    @Nullable
    public <T extends Extension> T addExtension(Class<T> extensionInterface) {
        ensureNotWritten();
        ExtensionFactory extensionFactory = extensionProviderManager.getExtensionFactory(extensionInterface);

        if (extensionFactory == null) {
            return null;
        }

        AbstractExtension extension = extensionFactory.newExtension();

        if (!extensionInterface.isInstance(extension)) {
            LOG.warn("Extension registered by factory for type '{}' is of incompatible type: {}",
                    extensionInterface.getName(), extension.getClass().getName());
            return null;
        }

        if (extensions == null) {
            extensions = new LinkedList<>();
        }

        extensions.add(new ExtensionMeta(extension, extensionFactory.getNamespace(), extensionFactory.getPrefix(),
                extensionFactory.getLocalName(), extensionFactory.isEmptyTag()));

        return extensionInterface.cast(extension);
    }

    void write() throws SitemapException {
        ensureNotWritten();
        written = true;
        try {
            StringWriter urlChunk = new StringWriter();
            XMLStreamWriter urlWriter = xmlWriterFactory.createXMLStreamWriter(urlChunk);
            urlWriter.setPrefix(XMLConstants.DEFAULT_NS_PREFIX, SitemapImpl.SITEMAP_NAMESPACE);
            urlWriter.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, "url", SitemapImpl.SITEMAP_NAMESPACE);
            urlWriter.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, "loc", SitemapImpl.SITEMAP_NAMESPACE);
            urlWriter.writeCharacters(location);
            urlWriter.writeEndElement();

            if (lastModified != null) {
                urlWriter.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, "lastmod", SitemapImpl.SITEMAP_NAMESPACE);
                urlWriter.writeCharacters(lastModified.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                urlWriter.writeEndElement();
            }
            if (changeFrequency != null) {
                urlWriter.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, "changefreq", SitemapImpl.SITEMAP_NAMESPACE);
                urlWriter.writeCharacters(changeFrequency.name().toLowerCase(Locale.ROOT));
                urlWriter.writeEndElement();
            }
            if (priority != null) {
                urlWriter.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX, "priority", SitemapImpl.SITEMAP_NAMESPACE);
                urlWriter.writeCharacters(String.valueOf(Math.max(Math.min(priority, 1.0), 0.0)));
                urlWriter.writeEndElement();
            }
            urlWriter.flush();

            // write the extensions as separate chunks to the same output
            if (extensions != null) {
                // use a single StringWriter and reset it for each extension to save memory
                StringWriter extensionChunk = new StringWriter();
                for (ExtensionMeta extension : extensions) {
                    writeExtension(extensionChunk, extension);
                    urlChunk.append(extensionChunk.asCharSequence());
                    extensionChunk.reset();
                }
            }

            urlWriter.writeEndElement();
            urlWriter.flush();

            out.append(urlChunk.asCharSequence());
        } catch (XMLStreamException ex) {
            LOG.warn("Failed to serialize url", ex);
        } catch (IOException ex) {
            throw new SitemapException(ex);
        }
    }

    private void ensureNotWritten() {
        if (written) {
            throw new IllegalStateException("Url already written");
        }
    }

    private void writeExtension(StringWriter out, ExtensionMeta extension) {
        try {
            XMLStreamWriter extensionWriter = xmlWriterFactory.createXMLStreamWriter(out);
            extensionWriter.setPrefix(extension.prefix, extension.namespace);
            if (extension.emptyTag) {
                extensionWriter.writeEmptyElement(extension.prefix, extension.localName, extension.namespace);
            } else {
                extensionWriter.writeStartElement(extension.prefix, extension.localName, extension.namespace);
            }
            extension.extension.writeTo(new ExtensionWriter(extensionWriter, extension.namespace));
            if (!extension.emptyTag) {
                extensionWriter.writeEndElement();
            } else {
                // in order to properly close the empty tag at the end of the xml-fragment
                extensionWriter.writeCharacters("");
            }
            extensionWriter.flush();
        } catch (XMLStreamException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Failed to serialize extension {}", extension.extension.getClass().getName(), ex);
            } else {
                LOG.warn("Failed to serialize extension {}: {}", extension.extension.getClass().getName(), ex.getMessage());
            }
            out.reset();
        }
    }

    private static class ExtensionMeta {
        private final AbstractExtension extension;
        private final String namespace;
        private final String prefix;
        private final String localName;
        private final boolean emptyTag;

        public ExtensionMeta(AbstractExtension extension, String namespace, String prefix, String localName,
                             boolean emptyTag) {
            this.extension = extension;
            this.namespace = namespace;
            this.prefix = prefix;
            this.localName = localName;
            this.emptyTag = emptyTag;
        }
    }

}
