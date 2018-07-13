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
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FeaturesServiceFactoryImplTest {
    @SuppressWarnings("unchecked")
    @Test
    public void testInitialize() {
        List<Object> featuresService = new ArrayList<Object>();

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.registerService(Mockito.isA(Class.class), Mockito.isA(Object.class), Mockito.isA(Dictionary.class)))
            .then(i -> { featuresService.add(i.getArgument(1)); return null; });

        FeaturesServiceFactoryImpl fsf = new FeaturesServiceFactoryImpl(bc);

        Map<String, Set<String>> bfm = new HashMap<>();
        bfm.put("foo:123", Collections.singleton("g:a:f1"));
        bfm.put("bar:1.2.3", Collections.singleton(null));
        bfm.put("incomplete", Collections.singleton("g:a:f1"));
        bfm.put("tar:0.0.0", new HashSet<>(Arrays.asList("g:a:f1", null, "g:a:f2")));
        fsf.initialize(bfm);

        Features features = (Features) featuresService.get(0);
        assertEquals(Collections.singleton("g:a:f1"), features.getFeaturesForBundle("foo", new Version(123,0,0)));
        assertEquals(Collections.singleton(null), features.getFeaturesForBundle("bar", new Version(1,2,3)));
        assertEquals(new HashSet<>(Arrays.asList(null, "g:a:f1", "g:a:f2")),
                features.getFeaturesForBundle("tar", new Version(0,0,0)));
        assertNull(features.getFeaturesForBundle("incomplete", new Version(0,0,0)));
    }
}
