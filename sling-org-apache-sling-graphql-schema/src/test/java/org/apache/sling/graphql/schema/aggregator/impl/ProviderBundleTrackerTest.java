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

import org.apache.commons.io.IOUtils;
import org.apache.sling.graphql.schema.aggregator.LogCapture;
import org.apache.sling.graphql.schema.aggregator.U;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import ch.qos.logback.classic.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Optional;

public class ProviderBundleTrackerTest {
    private ProviderBundleTracker tracker;
    private static long bundleId;

    @Before
    public void setup() {
        bundleId = 0;
        final BundleContext ctx = mock(BundleContext.class);
        tracker = new ProviderBundleTracker(ctx);
    }

    @Test
    public void addBundle() throws Exception {
        final Bundle a = U.mockProviderBundle("A", ++bundleId, "1.txt");
        tracker.addingBundle(a, null);
        assertEquals(1, tracker.getSchemaProviders().size());

        final Partial s = tracker.getSchemaProviders().values().iterator().next();
        assertTrue(s.toString().contains(a.getSymbolicName()));
        assertTrue(s.toString().contains("1.txt"));
    }

    @Test
    public void addAndRemoveBundles() throws Exception {
        final Bundle a = U.mockProviderBundle("A", ++bundleId, "1.graphql.txt");
        final Bundle b = U.mockProviderBundle("B", ++bundleId, "2.txt", "1.txt");
        tracker.addingBundle(a, null);
        tracker.addingBundle(b, null);
        assertEquals(3, tracker.getSchemaProviders().size());
        tracker.removedBundle(b, null, null);
        assertEquals(1, tracker.getSchemaProviders().size());
        tracker.removedBundle(a, null, null);
        assertEquals(0, tracker.getSchemaProviders().size());
        tracker.removedBundle(a, null, null);
        assertEquals(0, tracker.getSchemaProviders().size());
    }

    @Test
    public void duplicatePartialName() throws Exception {
        final LogCapture capture = new LogCapture(ProviderBundleTracker.class.getName(), true);
        final Bundle a = U.mockProviderBundle("A", ++bundleId, "TT.txt");
        final Bundle b = U.mockProviderBundle("B", ++bundleId, "TT.txt", "another.x");
        tracker.addingBundle(a, null);
        tracker.addingBundle(b, null);
        capture.assertContains(Level.WARN, "Partial provider with name TT already present");
        assertEquals(2, tracker.getSchemaProviders().size());
    }

    private void assertSectionContent(Partial p, String name, String expected) throws IOException {
        final Optional<Partial.Section> os = p.getSection(name);
        assertTrue("Expecting section " + name, os.isPresent());
        assertEquals(expected, IOUtils.toString(os.get().getContent()).trim());
    }
 
    @Test
    public void getSectionsContent() throws IOException {
        final Bundle a = U.mockProviderBundle("A", ++bundleId, "1.txt");
        tracker.addingBundle(a, null);
        final Partial p = tracker.getSchemaProviders().values().iterator().next();
        assertSectionContent(p, PartialConstants.S_QUERY, "Fake query for 1.txt");
    }
}