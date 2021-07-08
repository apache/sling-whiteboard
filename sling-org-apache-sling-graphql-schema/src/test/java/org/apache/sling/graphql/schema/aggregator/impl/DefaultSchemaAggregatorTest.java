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
import org.apache.sling.graphql.schema.aggregator.U;
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
    public void severalProviders() throws Exception{
        final StringWriter target = new StringWriter();
        tracker.addingBundle(U.mockProviderBundle("A", 1, "1.txt", "2.z.w", "3abc", "4abc"), null);
        tracker.addingBundle(U.mockProviderBundle("B", 2, "B1a.txt", "B2.xy"), null);
        dsa.aggregate(target, "B1a", "B2", "2.z");
        assertContainsIgnoreCase("schema aggregated by DefaultSchemaAggregator", target.toString());

        try(InputStream is = getClass().getResourceAsStream("/several-providers-output.txt")) {
            assertNotNull("Expecting test resource to be present", is);
            final String expected = IOUtils.toString(is, "UTF-8");
            assertEquals(expected, target.toString().trim());
        }
    }

    @Test
    public void regexpSelection() throws Exception {
        final StringWriter target = new StringWriter();
        tracker.addingBundle(U.mockProviderBundle("A", 1, "a.authoring.1.txt", "a.authoring.2.txt", "3.txt", "4.txt"), null);
        tracker.addingBundle(U.mockProviderBundle("B", 2, "B1.txt", "B.authoring.txt"), null);
        dsa.aggregate(target, "B1", "/.*\\.authoring.*/");
        assertContainsIgnoreCase("schema aggregated by DefaultSchemaAggregator", target.toString());
        U.assertPartialsFoundInSchema(target.toString(), "a.authoring.1", "a.authoring.2", "B.authoring", "B1");
    }
}