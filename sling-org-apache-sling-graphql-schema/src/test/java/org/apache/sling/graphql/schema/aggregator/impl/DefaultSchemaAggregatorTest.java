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
package org.apache.sling.graphql.schema.aggregator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.mockito.Mockito.mock;

public class DefaultSchemaAggregatorTest {
    private DefaultSchemaAggregator dsa;
    private ProviderBundleTracker tracker;

    @Before
    public void setup() throws Exception {
        dsa = new DefaultSchemaAggregator();
        final BundleContext ctx = mock(BundleContext.class);
        dsa.activate(ctx);
        final Field f = dsa.getClass().getDeclaredField("tracker");
        f.setAccessible(true);
        tracker = (ProviderBundleTracker)f.get(dsa);
    }

    private void assertContainsIgnoreCase(String substring, String source) {
        assertTrue("Expecting '" + substring + "' in source string ", source.toLowerCase().contains(substring.toLowerCase()));
    }

    @Test
    public void noProviders() throws Exception{
        final StringWriter target = new StringWriter();
        final IOException iox = assertThrows(IOException.class, () -> dsa.aggregate(target, "Aprov", "Bprov"));
        assertContainsIgnoreCase("missing providers", iox.getMessage());
        assertContainsIgnoreCase("Aprov", iox.getMessage());
        assertContainsIgnoreCase("Bprov", iox.getMessage());
        assertContainsIgnoreCase("schema aggregated by DefaultSchemaAggregator", target.toString());
    }

    @Test
    public void twoProviders() throws Exception{
        final StringWriter target = new StringWriter();
        tracker.addingBundle(U.testBundle("A", 1, 4), null);
        tracker.addingBundle(U.testBundle("B", 2, 2), null);
        dsa.aggregate(target, "B/path/2/resource/1", "A/path/1/resource/3");
        assertContainsIgnoreCase("schema aggregated by DefaultSchemaAggregator", target.toString());

        try(InputStream is = getClass().getResourceAsStream("/two-providers-output.txt")) {
            assertNotNull("Expecting test resource to be present", is);
            final String expected = IOUtils.toString(is, "UTF-8");
            assertEquals(expected, target.toString().trim());
        }
    }
}