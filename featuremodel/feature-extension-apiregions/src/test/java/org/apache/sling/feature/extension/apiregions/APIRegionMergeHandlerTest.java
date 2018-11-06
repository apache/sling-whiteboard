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
package org.apache.sling.feature.extension.apiregions;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.Test;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class APIRegionMergeHandlerTest {
    @Test
    public void testCanMerge() {
        APIRegionMergeHandler armh = new APIRegionMergeHandler();

        Extension ex = new Extension(ExtensionType.JSON, "api-regions", false);
        assertTrue(armh.canMerge(ex));
        assertFalse(armh.canMerge(new Extension(ExtensionType.JSON, "foo", false)));
    }

    @Test
    public void testAPIRegionMerging() {
        APIRegionMergeHandler armh = new APIRegionMergeHandler();

        Feature tf = new Feature(ArtifactId.fromMvnId("x:t:1"));
        Feature sf = new Feature(ArtifactId.fromMvnId("y:s:2"));

        Extension tgEx = new Extension(ExtensionType.JSON, "api-regions", false);
        tgEx.setJSON("[{\"name\":\"global\","
                + "\"exports\": [\"a.b.c\",\"d.e.f\"]},"
                + "{\"name\":\"internal\","
                + "\"exports\":[\"xyz\"],"
                + "\"org-feature\":\"some:feature:1\"}]");

        Extension srEx = new Extension(ExtensionType.JSON, "api-regions", false);
        srEx.setJSON("[{\"name\":\"global\","
                + "\"exports\": [\"test\"]},"
                + "{\"name\":\"something\","
                + "\"exports\": [\"a.ha\"],"
                + "\"org-feature\": \"different:feature:1\"}]");

        armh.merge(null, tf, sf, tgEx, srEx);

        String expectedJSON = "[{\"name\":\"global\","
                + "\"exports\": [\"a.b.c\",\"d.e.f\"]},"
                + "{\"name\":\"internal\","
                + "\"exports\":[\"xyz\"],"
                + "\"org-feature\":\"some:feature:1\"},"
                + "{\"name\":\"global\","
                + "\"org-feature\":\"y:s:2\","
                + "\"exports\": [\"test\"]},"
                + "{\"name\":\"something\","
                + "\"exports\": [\"a.ha\"],"
                + "\"org-feature\": \"different:feature:1\"}]";
        JsonReader er = Json.createReader(new StringReader(expectedJSON));
        JsonReader ar = Json.createReader(new StringReader(tgEx.getJSON()));
        JsonArray ea = er.readArray();
        JsonArray aa = ar.readArray();

        assertEquals(ea, aa);
    }

    @Test
    public void testRegionExportsInheritance() throws Exception {
        APIRegionMergeHandler armh = new APIRegionMergeHandler();

        Feature tf = new Feature(ArtifactId.fromMvnId("x:t:1"));
        Feature sf = new Feature(ArtifactId.fromMvnId("y:s:2"));

        Extension srEx = new Extension(ExtensionType.JSON, "api-regions", false);
        srEx.setJSON("[{\"name\":\"global\","
                + "\"exports\": [\"a.b.c\",\"d.e.f\"]},"
                + "{\"name\":\"deprecated\","
                + "\"exports\":[\"klm\",\"qrs\"]},"
                + "{\"name\":\"internal\","
                + "\"exports\":[\"xyz\"]},"
                + "{\"name\":\"forbidden\","
                + "\"exports\":[\"abc\",\"klm\"]},"
                + "{\"name\":\"internal\","
                + "\"exports\":[\"test\"],"
                + "\"org-feature\":\"an.other:feature:123\"}]");

        armh.merge(null, tf, sf, null, srEx);

        Extension tgEx = tf.getExtensions().iterator().next();

        String expectedJSON = "[{\"name\":\"global\",\"org-feature\":\"y:s:2\",\"exports\":[\"a.b.c\",\"d.e.f\"]},"
                + "{\"name\":\"deprecated\",\"org-feature\":\"y:s:2\",\"exports\":[\"klm\",\"qrs\"]},"
                + "{\"name\":\"internal\",\"org-feature\":\"y:s:2\",\"exports\":[\"xyz\",\"klm\",\"qrs\"]},"
                + "{\"name\":\"forbidden\",\"org-feature\":\"y:s:2\",\"exports\":[\"abc\",\"klm\",\"qrs\",\"xyz\"]},"
                + "{\"name\":\"internal\",\"org-feature\":\"an.other:feature:123\",\"exports\":[\"test\"]}]";
        JsonReader er = Json.createReader(new StringReader(expectedJSON));
        JsonReader ar = Json.createReader(new StringReader(tgEx.getJSON()));
        JsonArray ea = er.readArray();
        JsonArray aa = ar.readArray();

        assertEquals(ea, aa);
    }
}
