/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.apiregions.io.json;

import static org.apache.sling.feature.ExtensionType.JSON;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import static java.util.Objects.requireNonNull;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.apiregions.ApiRegion;
import org.apache.sling.feature.apiregions.ApiRegions;

/**
 * <code>api-regions</code> JSON format parser implementation.
 */
public final class ApiRegionsJSONParser implements JSONConstants {

    private ApiRegionsJSONParser() {
        // this class must not be instantiated from outside
    }

    /**
     * Extracts and parses an <code>api-regions</code> JSON Extension from a Feature Model,
     * mapping it to the related in-memory representation.
     *
     * @param feature the Feature Model containing the <code>api-regions</code> to parse.
     * @return the related in-memory representation of the <code>api-regions</code>,
     * null if the <code>api-regions</code> extension does not exist in the input Feature Model.
     */
    public static ApiRegions parseApiRegions(Feature feature) {
        requireNonNull(feature, "Impossible to extract api-regions from a null feature");

        Extensions extensions = feature.getExtensions();
        Extension apiRegionsExtension = extensions.getByName(API_REGIONS_KEY);
        if (apiRegionsExtension == null) {
            return null;
        }

        return parseApiRegions(apiRegionsExtension);
    }

    /**
     * Extracts and parses an <code>api-regions</code> JSON Extension, mapping it to the related in-memory representation.
     *
     * @param apiRegionsExtension the <code>api-regions</code> JSON Extension, must named <code>api-regions</code> and of type JSON.
     * @return the related in-memory representation of the <code>api-regions</code>
     */
    public static ApiRegions parseApiRegions(Extension apiRegionsExtension) {
        requireNonNull(apiRegionsExtension, "Impossible to extract api-regions from a null extension");

        if (!API_REGIONS_KEY.equals(apiRegionsExtension.getName())) {
            throw new IllegalArgumentException(apiRegionsExtension.getName() + " is not a recognised api-regions extension");
        }
        if (JSON != apiRegionsExtension.getType()) {
            throw new IllegalArgumentException("api-regions extension must be of JSON type, "
                                               + apiRegionsExtension.getType()
                                               + " is not a recognised as valid api-regions extension");
        }

        return parseApiRegions(apiRegionsExtension.getJSON());
    }

    /**
     * Parses an <code>api-regions</code> JSON string representation, mapping it to the related in-memory representation.
     *
     * @param jsonRepresentation the <code>api-regions</code> JSON string representation
     * @return the related in-memory representation of the <code>api-regions</code>
     */
    public static ApiRegions parseApiRegions(String jsonRepresentation) {
        requireNonNull(jsonRepresentation, "Impossible to extract api-regions from a null JSON representation");

        ApiRegions apiRegions = new ApiRegions();

        // pointers
        Event event;
        String regionName;
        Collection<String> apis;

        JsonParser parser = Json.createParser(new StringReader(jsonRepresentation));
        if (Event.START_ARRAY != parser.next()) {
            throw new IllegalStateException("Expected 'api-region' element to start with an Array: "
                                             + parser.getLocation());
        }

        while (Event.END_ARRAY != (event = parser.next())) {
            if (Event.START_OBJECT != event) {
                throw new IllegalStateException("Expected 'api-region' data to start with an Object: "
                                                + parser.getLocation());
            }

            regionName = null;
            apis = null;

            while (Event.END_OBJECT != (event = parser.next())) {
                if (Event.KEY_NAME == event) {
                    switch (parser.getString()) {
                        case NAME_KEY:
                            parser.next();
                            regionName = parser.getString();
                            break;

                        case EXPORTS_KEY:
                            // start array
                            parser.next();

                            apis = new ArrayList<>();

                            while (parser.hasNext() && Event.VALUE_STRING == parser.next()) {
                                String api = parser.getString();
                                // skip comments
                                if ('#' != api.charAt(0)) {
                                    apis.add(api);
                                }
                            }

                            break;

                        default:
                            break;
                    }
                }
            }

            if (regionName != null
                    && !regionName.isEmpty()
                    && apis != null
                    && !apis.isEmpty()) {
                ApiRegion apiRegion = apiRegions.addNew(regionName);
                apiRegion.addAll(apis);
            }
        }

        return apiRegions;
    }

}
