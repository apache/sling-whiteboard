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

public class FeatureServiceImplTest {
    /*
    @Test
    public void testFeatureService() {
        Map<Entry<String, Version>, Set<String>> bif = new HashMap<>();

        String f1 = "gid:aid:1.0.0:myfeature:slingfeature";
        bif.put(new AbstractMap.SimpleEntry<String,Version>("mybsn", new Version(1,2,3)),
                new HashSet<>(Arrays.asList(f1)));
        bif.put(new AbstractMap.SimpleEntry<String,Version>("mybsn2", new Version(4,5,6)),
                new HashSet<>(Arrays.asList(f1, null)));

        String f2 = "gid:aid2:1.0.0";
        bif.put(new AbstractMap.SimpleEntry<String,Version>("mybsn", new Version(7,8,9)),
                new HashSet<>(Collections.singleton(f2)));

        Features fs = new FeaturesServiceImpl(bif);

        assertEquals(Collections.singleton(f1), fs.getFeaturesForBundle("mybsn", new Version(1,2,3)));
        assertEquals(new HashSet<>(Arrays.asList(null, f1)),
                fs.getFeaturesForBundle("mybsn2", new Version(4,5,6)));
        assertEquals(Collections.singleton(f2), fs.getFeaturesForBundle("mybsn", new Version(7,8,9)));
        assertNull(fs.getFeaturesForBundle("mybsn2", new Version(1,2,3)));
    }
    */
}
