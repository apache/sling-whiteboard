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

import static org.junit.Assert.assertEquals;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.sling.feature.apiregions.ApiRegion;
import org.apache.sling.feature.apiregions.ApiRegions;
import org.junit.Test;

public final class ApiRegionsJSONSerializerTest {

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
    public void serialization() {
        String expected = "[\n" + 
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

        ApiRegions apiRegions = new ApiRegions();

        ApiRegion base = apiRegions.addNewRegion("base");
        base.add("org.apache.felix.inventory");
        base.add("org.apache.felix.metatype");

        ApiRegion extended = apiRegions.addNewRegion("extended");
        extended.add("org.apache.felix.scr.component");
        extended.add("org.apache.felix.scr.info");

        StringWriter writer = new StringWriter();
        ApiRegionsJSONSerializer.serializeApiRegions(apiRegions, writer);

        String actual = writer.toString();

        assertEquals(expected, actual);
    }

}
