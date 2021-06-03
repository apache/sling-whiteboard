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
package org.apache.sling.sitemap.impl.builder.extensions;

import org.apache.sling.sitemap.builder.extensions.AbstractExtension;
import org.apache.sling.sitemap.builder.extensions.AlternateLanguageExtension;
import org.apache.sling.sitemap.builder.extensions.ExtensionProvider;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Locale;

@Component(
        property = {
                ExtensionProvider.PROPERTY_INTERFACE + "=org.apache.sling.sitemap.builder.extensions.AlternateLanguageExtension",
                ExtensionProvider.PROPERTY_PREFIX + "=xhtml",
                ExtensionProvider.PROPERTY_NAMESPACE + "=http://www.w3.org/1999/xhtml",
                ExtensionProvider.PROPERTY_LOCAL_NAME + "=link",
                ExtensionProvider.PROPERTY_EMPTY_TAG + "=true"
        }
)
public class AlternateLanguageExtensionProvider implements ExtensionProvider {

    @Override
    @NotNull
    public AbstractExtension newInstance() {
        return new ExtensionImpl();
    }

    public static class ExtensionImpl extends AbstractExtension implements AlternateLanguageExtension {

        private String hreflang;
        private String href;

        private static <T> T required(T object, String message) throws XMLStreamException {
            if (object == null) {
                throw new XMLStreamException(message);
            }
            return object;
        }

        @Override
        @NotNull
        public AlternateLanguageExtension setLocale(@NotNull Locale locale) {
            hreflang = locale.toLanguageTag();
            return this;
        }

        @Override
        @NotNull
        public AlternateLanguageExtension setDefaultLocale() {
            hreflang = "x-default";
            return this;
        }

        @Override
        @NotNull
        public AlternateLanguageExtension setHref(@NotNull String location) {
            href = location;
            return this;
        }

        @Override
        public void writeTo(@NotNull XMLStreamWriter writer) throws XMLStreamException {
            writer.writeAttribute("rel", "alternate");
            writer.writeAttribute("hreflang", required(hreflang, "hreflang is missing"));
            writer.writeAttribute("href", required(href, "href is missing"));
        }
    }
}
