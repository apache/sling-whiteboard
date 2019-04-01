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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.apiregions.ApiRegion;
import org.apache.sling.feature.apiregions.ApiRegions;
import org.junit.Test;

public class ApiRegionsJSONParserTest {

    @Test(expected = NullPointerException.class)
    public void canNotParseNullFeature() {
        ApiRegionsJSONParser.parseApiRegions((Feature) null);
    }

    @Test(expected = NullPointerException.class)
    public void canNotParseNullExtension() {
        ApiRegionsJSONParser.parseApiRegions((Extension) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void canNotParseInvalidExtensionName() {
        Extension extension = new Extension(ExtensionType.JSON, "invalid", false);
        ApiRegionsJSONParser.parseApiRegions(extension);
    }

    @Test(expected = IllegalArgumentException.class)
    public void canNotParseInvalidExtensionType() {
        Extension extension = new Extension(ExtensionType.TEXT, "api-regions", false);
        ApiRegionsJSONParser.parseApiRegions(extension);
    }

    @Test(expected = NullPointerException.class)
    public void canNotParseNullJsonRepresentation() {
        ApiRegionsJSONParser.parseApiRegions((String) null);
    }

    @Test
    public void parseApiRegions() {
        Extension extension = new Extension(ExtensionType.JSON, "api-regions", false);
        extension.setJSON("[\n" + 
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
                            "]");
        Feature feature = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.apiregions:1.0.0"));
        feature.getExtensions().add(extension);

        ApiRegions apiRegions = ApiRegionsJSONParser.parseApiRegions(feature);

        ApiRegion base = apiRegions.getByName("base");
        assertNull(base.getParent());
        assertTrue(base.contains("org.apache.felix.inventory"));
        assertTrue(base.contains("org.apache.felix.metatype"));

        ApiRegion extended = apiRegions.getByName("extended");
        assertNotNull(extended.getParent());
        assertSame(base, extended.getParent());
        assertTrue(extended.contains("org.apache.felix.inventory"));
        assertTrue(extended.contains("org.apache.felix.metatype"));
        assertTrue(extended.contains("org.apache.felix.scr.component"));
        assertTrue(extended.contains("org.apache.felix.scr.info"));
    }

}
