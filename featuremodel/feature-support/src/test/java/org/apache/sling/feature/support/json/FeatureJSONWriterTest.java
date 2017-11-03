/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.support.json;

import org.apache.sling.feature.Feature;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class FeatureJSONWriterTest {

    @Test public void testRead() throws Exception {
        final Feature feature = U.readFeature("test");
        final Feature readFeature;
        try ( final StringWriter writer = new StringWriter() ) {
            FeatureJSONWriter.write(writer, feature);
            try ( final StringReader reader = new StringReader(writer.toString()) ) {
                readFeature = FeatureJSONReader.read(reader, null);
            }
        }
        assertEquals(feature.getId(), readFeature.getId());
        assertEquals(Arrays.asList("org.osgi.service.http.runtime.HttpServiceRuntime"),
                U.findCapability(readFeature.getCapabilities(), "osgi.service").getAttributes().get("objectClass"));
    }

}
