/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.contentparser.xml.jcr.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.contentparser.api.ContentHandler;
import org.apache.sling.contentparser.api.ContentParser;
import org.apache.sling.contentparser.api.ParseException;
import org.apache.sling.contentparser.api.ParserOptions;
import org.osgi.service.component.annotations.Component;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

@Component(
        service = ContentParser.class,
        property = {
                ContentParser.SERVICE_PROPERTY_CONTENT_TYPE + "=jcr-xml"
        }
)
public final class JCRXMLContentParser implements ContentParser {

    private final SAXParserFactory saxParserFactory;

    public JCRXMLContentParser() {
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
    }

    @Override
    public void parse(ContentHandler handler, InputStream is, ParserOptions parserOptions) throws IOException, ParseException {
        try {
            XmlHandler xmlHandler = new XmlHandler(handler, parserOptions);
            SAXParser parser = saxParserFactory.newSAXParser();
            parser.parse(is, xmlHandler);
            if (xmlHandler.hasError()) {
                throw xmlHandler.getError();
            }
        } catch (ParserConfigurationException | SAXException ex) {
            throw new ParseException("Error parsing JCR XML content.", ex);
        }
    }

    /**
     * Decodes element or attribute names.
     *
     * @param qname qname
     * @return Decoded name
     */
    static String decodeName(String qname) {
        return ISO9075.decode(qname);
    }

    /**
     * Parses XML stream to Map.
     */
    class XmlHandler extends DefaultHandler {
        private final ContentHandler contentHandler;
        private final ParserOptions parserOptions;
        private final Deque<String> paths = new ArrayDeque<>();
        private final Set<String> ignoredPaths = new HashSet<>();
        private SAXParseException error;

        XmlHandler(ContentHandler contentHandler, ParserOptions parserOptions) {
            this.contentHandler = contentHandler;
            this.parserOptions = parserOptions;
        }

        boolean hasError() {
            return error != null;
        }

        SAXParseException getError() {
            return error;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String resourceName = decodeName(qName);

            // generate path for element
            String path;
            if (paths.isEmpty()) {
                path = "/";
            } else {
                String parentPath = paths.peek();
                path = parentPath.endsWith("/") ? parentPath + resourceName : parentPath + "/" + resourceName;
                if (parserOptions.getIgnoreResourceNames().contains(resourceName)) {
                    ignoredPaths.add(path);
                }
            }
            paths.push(path);

            // skip further processing if this path or a parent path is ignored
            if (isIgnoredPath(path)) {
                return;
            }

            // get properties
            Map<String, Object> properties = new HashMap<>();
            for (int i = 0; i < attributes.getLength(); i++) {
                String propertyName = removePrefixFromPropertyName(parserOptions.getRemovePropertyNamePrefixes(),
                        decodeName(attributes.getQName(i)));
                if (!parserOptions.getIgnorePropertyNames().contains(propertyName)) {
                    Object value = JcrXmlValueConverter.parseValue(propertyName, attributes.getValue(i));
                    if (value != null) {
                        properties.put(propertyName, value);
                    }
                }
            }
            String defaultPrimaryType = parserOptions.getDefaultPrimaryType();
            if (defaultPrimaryType != null) {
                if (!properties.containsKey(JcrConstants.JCR_PRIMARYTYPE)) {
                    properties.put(JcrConstants.JCR_PRIMARYTYPE, defaultPrimaryType);
                }
            }
            contentHandler.resource(path, properties);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            paths.pop();
        }

        @Override
        public void error(SAXParseException ex) {
            this.error = ex;
        }

        @Override
        public void fatalError(SAXParseException ex) {
            this.error = ex;
        }

        private boolean isIgnoredPath(String path) {
            if (path == null || path.isEmpty()) {
                return false;
            }
            if (ignoredPaths.contains(path)) {
                return true;
            }
            if (path.contains("/")) {
                String parentPath = path.substring(0, path.lastIndexOf("/"));
                return isIgnoredPath(parentPath);
            } else {
                return isIgnoredPath(path);
            }

        }

        private String removePrefixFromPropertyName(Set<String> prefixes, String propertyName) {
            for (String prefix : prefixes) {
                if (propertyName.startsWith(prefix)) {
                    return propertyName.substring(prefix.length());
                }
            }
            return propertyName;
        }

    }

}
