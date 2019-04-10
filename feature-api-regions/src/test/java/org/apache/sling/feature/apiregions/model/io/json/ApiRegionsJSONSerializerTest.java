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
package org.apache.sling.feature.apiregions.model.io.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.apiregions.model.ApiRegion;
import org.apache.sling.feature.apiregions.model.ApiRegions;
import org.apache.sling.feature.apiregions.model.io.json.ApiRegionsJSONSerializer;
import org.apache.sling.feature.apiregions.model.io.json.JSONConstants;
import org.junit.Before;
import org.junit.Test;

public final class ApiRegionsJSONSerializerTest {

    private ApiRegions apiRegions;

    private String expected;

    @Before
    public void setUp() {
        apiRegions = new ApiRegions();

        ApiRegion base = apiRegions.addNew("base");
        base.add("org.apache.felix.inventory");
        base.add("org.apache.felix.metatype");

        ApiRegion extended = apiRegions.addNew("extended");
        extended.add("org.apache.felix.scr.component");
        extended.add("org.apache.felix.scr.info");

        expected = "[\n" + 
                "  {\n" + 
                "    \"name\":\"base\",\n" + 
                "    \"exports\":[\n" + 
                "      \"org.apache.felix.metatype\",\n" + 
                "      \"org.apache.felix.inventory\"\n" + 
                "    ]\n" + 
                "  },\n" + 
                "  {\n" + 
                "    \"name\":\"extended\",\n" + 
                "    \"exports\":[\n" + 
                "      \"org.apache.felix.scr.component\",\n" + 
                "      \"org.apache.felix.scr.info\"\n" + 
                "    ]\n" + 
                "  }\n" + 
                "]";
    }

    @Test(expected = NullPointerException.class)
    public void nullOutputStreamNotAccepted() {
        ApiRegionsJSONSerializer.serializeApiRegions(null, (OutputStream) null);
    }

    @Test(expected = NullPointerException.class)
    public void nullApiRegionsNotAccepted() {
        ApiRegionsJSONSerializer.serializeApiRegions(null, (Writer) null);
    }

    @Test(expected = NullPointerException.class)
    public void nullWriterNotAccepted() {
        ApiRegionsJSONSerializer.serializeApiRegions(new ApiRegions(), (Writer) null);
    }

    @Test
    public void stringSerialization() {
        StringWriter writer = new StringWriter();
        ApiRegionsJSONSerializer.serializeApiRegions(apiRegions, writer);

        String actual = writer.toString();

        assertEquals(expected, actual);
    }

    @Test
    public void extensionCreation() {
        Extension extension = ApiRegionsJSONSerializer.serializeApiRegions(apiRegions);
        extensionAssertions(extension);
    }

    @Test
    public void addExtensionToFeature() {
        Feature feature = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.apiregions:1.0.0"));

        ApiRegionsJSONSerializer.serializeApiRegions(apiRegions, feature);

        Extension extension = feature.getExtensions().getByName(JSONConstants.API_REGIONS_KEY);
        extensionAssertions(extension);
    }

    private void extensionAssertions(Extension extension) {
        assertNotNull(extension);
        assertEquals(ExtensionType.JSON, extension.getType());
        assertEquals(JSONConstants.API_REGIONS_KEY, extension.getName());
        assertFalse(extension.isRequired());
        assertTrue(extension.isOptional());
        assertEquals(expected, extension.getJSON());
    }

}
