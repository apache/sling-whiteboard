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
package org.apache.sling.contentparser.json.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.contentparser.api.ContentHandler;
import org.apache.sling.contentparser.api.ContentParser;
import org.apache.sling.contentparser.api.JsonParserFeature;
import org.apache.sling.contentparser.api.ParseException;
import org.apache.sling.contentparser.api.ParserHelper;
import org.apache.sling.contentparser.api.ParserOptions;
import org.osgi.service.component.annotations.Component;

@Component(
        property = {
                ContentParser.SERVICE_PROPERTY_CONTENT_TYPE + "=" + ContentParser.JSON_CONTENT_TYPE
        }
)
public class JsonContentParser implements ContentParser {

    @Override
    public void parse(ContentHandler handler, InputStream is, ParserOptions parserOptions) throws ParseException {
        final boolean jsonQuoteTicks = parserOptions.getJsonParserFeatures().contains(JsonParserFeature.QUOTE_TICK);

        /*
         * Implementation note: This parser uses JsonReader instead of the (more memory-efficient)
         * JsonParser Stream API because otherwise it would not be possible to report parent resources
         * including all properties properly before their children.
         */
        final JsonReaderFactory jsonReaderFactory =
                Json.createReaderFactory(
                        parserOptions.getJsonParserFeatures().contains(JsonParserFeature.COMMENTS) ?
                                new HashMap<String, Object>() {{
                                    put("org.apache.johnzon.supports-comments", true);
                                }} :
                                Collections.emptyMap()
                );
        JsonObject jsonObject = jsonQuoteTicks ? toJsonObjectWithJsonTicks(jsonReaderFactory, is) : toJsonObject(jsonReaderFactory, is);
        parse(handler, jsonObject, parserOptions, "/");
    }

    private JsonObject toJsonObject(JsonReaderFactory jsonReaderFactory, InputStream is) {
        try (JsonReader reader = jsonReaderFactory.createReader(is)) {
            return reader.readObject();
        } catch (JsonParsingException ex) {
            throw new ParseException("Error parsing JSON content: " + ex.getMessage(), ex);
        }
    }

    private JsonObject toJsonObjectWithJsonTicks(JsonReaderFactory jsonReaderFactory, InputStream is) {
        String jsonString;
        try {
            jsonString = IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ParseException("Error getting JSON string.", ex);
        }

        // convert ticks to double quotes
        jsonString = JsonTicksConverter.tickToDoubleQuote(jsonString);

        try (JsonReader reader = jsonReaderFactory.createReader(new StringReader(jsonString))) {
            return reader.readObject();
        } catch (JsonParsingException ex) {
            throw new ParseException("Error parsing JSON content: " + ex.getMessage(), ex);
        }
    }

    private void parse(ContentHandler handler, JsonObject object, ParserOptions parserOptions, String path) {
        // parse JSON object
        Map<String, Object> properties = new HashMap<>();
        Map<String, JsonObject> children = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
            String childName = entry.getKey();
            Object value = null;
            boolean ignore = false;
            try {
                value = convertValue(parserOptions, entry.getValue());
            } catch (ParseException ex) {
                if (parserOptions.getIgnoreResourceNames().contains(childName) || parserOptions.getIgnorePropertyNames()
                        .contains(removePrefixFromPropertyName(parserOptions.getRemovePropertyNamePrefixes(), childName))) {
                    ignore = true;
                } else {
                    throw ex;
                }
            }
            boolean isResource = (value instanceof JsonObject);
            if (!ignore) {
                if (isResource) {
                    ignore = parserOptions.getIgnoreResourceNames().contains(childName);
                } else {
                    for (String prefix : parserOptions.getRemovePropertyNamePrefixes()) {
                        if (childName.startsWith(prefix)) {
                            childName = childName.substring(prefix.length());
                            break;
                        }

                    }
                    ignore = parserOptions.getIgnorePropertyNames().contains(childName);
                }
            }
            if (!ignore) {
                if (isResource) {
                    children.put(childName, (JsonObject) value);
                } else {
                    properties.put(childName, value);
                }
            }
        }
        String defaultPrimaryType = parserOptions.getDefaultPrimaryType();
        if (defaultPrimaryType != null) {
            if (!properties.containsKey("jcr:primaryType")) {
                properties.put("jcr:primaryType", defaultPrimaryType);
            }
        }

        // report current JSON object
        handler.resource(path, properties);

        // parse and report children
        for (Map.Entry<String, JsonObject> entry : children.entrySet()) {
            String childPath = path.endsWith("/") ? path + entry.getKey() : path + "/" + entry.getKey();
            parse(handler, entry.getValue(), parserOptions, childPath);
        }
    }

    private Object convertValue(ParserOptions parserOptions, JsonValue value) {
        switch (value.getValueType()) {
            case STRING:
                String stringValue = ((JsonString) value).getString();
                if (parserOptions.isDetectCalendarValues()) {
                    Calendar calendar = ParserHelper.parseDate(stringValue);
                    if (calendar != null) {
                        return calendar;
                    }
                }
                return stringValue;
            case NUMBER:
                JsonNumber numberValue = (JsonNumber) value;
                if (numberValue.isIntegral()) {
                    return numberValue.longValue();
                } else {
                    return numberValue.bigDecimalValue();
                }
            case TRUE:
                return true;
            case FALSE:
                return false;
            case NULL:
                return null;
            case ARRAY:
                JsonArray arrayValue = (JsonArray) value;
                Object[] values = new Object[arrayValue.size()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = convertValue(parserOptions, arrayValue.get(i));
                }
                return ParserHelper.convertSingleTypeArray(values);
            case OBJECT:
                return value;
            default:
                throw new ParseException("Unexpected JSON value type: " + value.getValueType());
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
