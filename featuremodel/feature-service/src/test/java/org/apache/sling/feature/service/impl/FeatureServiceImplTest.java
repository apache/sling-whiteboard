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
package org.apache.sling.feature.service.impl;

import org.apache.sling.feature.service.Features;
import org.junit.Test;
import org.osgi.framework.Version;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FeatureServiceImplTest {
    @Test
    public void testFeatureService() {
        Map<Entry<String, Version>, String> bif = new HashMap<>();

        String f1 = "gid:aid:1.0.0:myfeature:slingfeature";
        bif.put(new AbstractMap.SimpleEntry<String,Version>("mybsn", new Version(1,2,3)), f1);
        bif.put(new AbstractMap.SimpleEntry<String,Version>("mybsn2", new Version(4,5,6)), f1);

        String f2 = "gid:aid2:1.0.0";
        bif.put(new AbstractMap.SimpleEntry<String,Version>("mybsn", new Version(7,8,9)), f2);

        Features fs = new FeaturesServiceImpl(bif);
        assertEquals(2, fs.listFeatures().size());

        assertEquals(f1, fs.getFeatureForBundle("mybsn", new Version(1,2,3)));
        assertEquals(f1, fs.getFeatureForBundle("mybsn2", new Version(4,5,6)));
        assertEquals(f2, fs.getFeatureForBundle("mybsn", new Version(7,8,9)));
        assertNull(fs.getFeatureForBundle("mybsn2", new Version(1,2,3)));
    }
}
