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
package org.apache.sling.contentparser.xml.internal;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.contentparser.api.ContentHandler;
import org.apache.sling.contentparser.api.ContentParser;
import org.apache.sling.contentparser.api.ParseException;
import org.apache.sling.contentparser.api.ParserHelper;
import org.apache.sling.contentparser.api.ParserOptions;
import org.osgi.service.component.annotations.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parses XML files that contains content fragments.
 * Instance of this class is thread-safe.
 */
@Component(
        property = {
                ContentParser.SERVICE_PROPERTY_CONTENT_TYPE + "=xml"
        }
)
public final class XMLContentParser implements ContentParser {

    private static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    private final DocumentBuilderFactory documentBuilderFactory;

    public XMLContentParser() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
    }

    @Override
    public void parse(ContentHandler handler, InputStream is, ParserOptions parserOptions) throws IOException, ParseException {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(is);
            parse(handler, doc.getDocumentElement(), parserOptions, null);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new ParseException("Error parsing JCR XML content.", ex);
        }
    }

    private void parse(ContentHandler handler, Element element, ParserOptions parserOptions, String parentPath) {
        // build node path
        String path;
        if (parentPath == null) {
            path = "/";
        } else {
            String name = getChildText(element, "name");
            if (StringUtils.isEmpty(name)) {
                throw new ParseException("Child node without name detected below path " + parentPath);
            }
            if (parserOptions.getIgnoreResourceNames().contains(name)) {
                return;
            }
            path = parentPath.endsWith("/") ? parentPath + name : parentPath + "/" + name;
        }

        Map<String, Object> properties = new HashMap<>();

        // primary node type and mixins
        String primaryType = getChildText(element, "primaryNodeType");
        if (StringUtils.isNotBlank(primaryType) && !parserOptions.getIgnorePropertyNames().contains("jcr:primaryType")) {
            properties.put("jcr:primaryType", primaryType);
        }
        String[] mixins = getChildTextArray(element, "mixinNodeType");
        if (mixins.length > 0 && !parserOptions.getIgnorePropertyNames().contains("jcr:mixinTypes")) {
            properties.put("jcr:mixinTypes", mixins);
        }

        // properties
        List<Element> propertyElements = getChildren(element, "property");
        for (Element propertyElement : propertyElements) {

            // property name
            String name = getChildText(propertyElement, "name");
            if (StringUtils.isBlank(name)) {
                throw new ParseException("Property without name detected at path " + path);
            }
            if (parserOptions.getIgnorePropertyNames().contains(name)) {
                continue;
            }

            // property type
            String type = getChildText(propertyElement, "type");
            if (StringUtils.isBlank(type)) {
                throw new ParseException("Property '" + name + "' has no type at path " + path);
            }

            // property value
            Object value;
            List<Element> valuesElements = getChildren(propertyElement, "values");
            if (!valuesElements.isEmpty()) {
                Element valuesElement = valuesElements.get(0);
                List<Element> valueElements = getChildren(valuesElement, "value");
                String[] stringValues = new String[valueElements.size()];
                for (int i = 0; i < valueElements.size(); i++) {
                    stringValues[i] = valueElements.get(i).getTextContent();
                }
                value = convertMultiValue(stringValues, type);
            } else {
                String stringValue = getChildText(propertyElement, "value");
                value = convertValue(stringValue, type);
            }
            properties.put(name, value);
        }

        String defaultPrimaryType = parserOptions.getDefaultPrimaryType();
        if (defaultPrimaryType != null) {
            if (!properties.containsKey(JCR_PRIMARY_TYPE)) {
                properties.put(JCR_PRIMARY_TYPE, defaultPrimaryType);
            }
        }
        handler.resource(path, properties);

        // child nodes
        List<Element> nodeElements = getChildren(element, "node");
        for (Element node : nodeElements) {
            parse(handler, node, parserOptions, path);
        }

    }

    private List<Element> getChildren(Element element, String childName) {
        List<Element> result = new ArrayList<>();
        NodeList children = element.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element childElement = (Element) child;
                if (StringUtils.equals(childElement.getNodeName(), childName)) {
                    result.add(childElement);
                }
            }
        }
        return result;
    }

    private String getChildText(Element element, String childName) {
        List<Element> children = getChildren(element, childName);
        if (children.isEmpty()) {
            return null;
        } else if (children.size() == 1) {
            return children.get(0).getTextContent();
        } else {
            throw new ParseException("Found multiple elements with name '" + childName + "': " + children.size());
        }
    }

    private String[] getChildTextArray(Element element, String childName) {
        List<Element> children = getChildren(element, childName);
        String[] result = new String[children.size()];
        for (int i = 0; i < children.size(); i++) {
            result[i] = children.get(i).getTextContent();
        }
        return result;
    }

    private Object convertValue(String value, String type) {
        switch (type) {
            case "String":
            case "Name":
            case "Path":
            case "Reference":
            case "WeakReference":
            case "URI":
                return value;
            case "Long":
                return Long.valueOf(value);
            case "Double":
                return Double.valueOf(value);
            case "Date":
                return ParserHelper.parseDate(value);
            case "Boolean":
                return Boolean.valueOf(value);
            case "Decimal":
                return new BigDecimal(value);
            default:
                throw new ParseException(String.format("Unsupported property type: %s.", type));
        }
    }

    private Object convertMultiValue(String[] values, String type) {
        Object[] result = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = convertValue(values[i], type);
        }
        return ParserHelper.convertSingleTypeArray(result);
    }

}
