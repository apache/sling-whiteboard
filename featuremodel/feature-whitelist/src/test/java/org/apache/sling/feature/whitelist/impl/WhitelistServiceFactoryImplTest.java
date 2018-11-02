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
package org.apache.sling.feature.whitelist.impl;

public class WhitelistServiceFactoryImplTest {
    /*
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testWhitelistServiceFactory() {
        List<ResolverHookFactory> resolverHookFactory = new ArrayList<>();
        Map<String, Map<String, Set<String>>> wlsCfg = new HashMap<>();

        ServiceTracker st = Mockito.mock(ServiceTracker.class);
        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.registerService(Mockito.isA(Class.class), Mockito.isA(Object.class), Mockito.isA(Dictionary.class)))
            .then(i -> { resolverHookFactory.add(i.getArgument(1)); return null; });

        WhitelistServiceFactory wsf = new WhitelistServiceFactoryImpl(bc, st) {
            @Override
            WhitelistService createWhitelistService(Map<String, Set<String>> packages, Map<String, Set<String>> regions) {
                wlsCfg.put("packages", packages);
                wlsCfg.put("regions", regions);
                return super.createWhitelistService(packages, regions);
            }
        };

        Map<String, Map<String, Set<String>>> m = new HashMap<>();
        Map<String, Set<String>> packages = new HashMap<>();
        packages.put("region1", new HashSet<>(Arrays.asList("org.foo", "org.bar")));
        packages.put("region2", Collections.singleton("org.foo.bar"));
        m.put("packages", packages);

        Map<String, Set<String>> regions = new HashMap<>();
        regions.put("f1", new HashSet<String>(Arrays.asList("region1", "region3")));
        regions.put("f2", Collections.singleton("region2"));
        regions.put("f3", Collections.singleton("region4"));
        regions.put("f4", Collections.singleton("region2"));
        m.put("regions", regions);

        wsf.initialize(m);

        assertEquals(wlsCfg, m);

        ResolverHookFactory rhf = resolverHookFactory.get(0);
        assertTrue(rhf instanceof WhitelistEnforcer);
    }
    */
}
