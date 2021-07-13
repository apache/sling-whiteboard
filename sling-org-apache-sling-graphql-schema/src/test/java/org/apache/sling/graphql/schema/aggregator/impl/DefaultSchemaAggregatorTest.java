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
import java.util.Optional;
import java.util.stream.Stream;

import graphql.language.TypeDefinition;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

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
        final String sdl = target.toString().trim();
        assertContainsIgnoreCase("schema aggregated by DefaultSchemaAggregator", sdl);

        try(InputStream is = getClass().getResourceAsStream("/several-providers-output.txt")) {
            assertNotNull("Expecting test resource to be present", is);
            final String expected = IOUtils.toString(is, "UTF-8");
            assertEquals(expected, sdl);
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

    @Test
    public void parseResult() throws Exception {
        final StringWriter target = new StringWriter();
        tracker.addingBundle(U.mockProviderBundle("SDL", 1, "a.sdl.txt", "b.sdl.txt", "c.sdl.txt"), null);

        dsa.aggregate(target, "/.*/");

        // Parse the output with a real SDL parser
        final String sdl = target.toString();
        final TypeDefinitionRegistry reg = new SchemaParser().parse(sdl);

        // And make sure it contains what we expect
        assertTrue(reg.getDirectiveDefinition("fetcher").isPresent());
        assertTrue(reg.getType("SlingResourceConnection").isPresent());
        assertTrue(reg.getType("PageInfo").isPresent());
        
        final Optional<TypeDefinition> query = reg.getType("Query");
        assertTrue("Expecting Query", query.isPresent());
        assertTrue(query.get().getChildren().toString().contains("oneSchemaResource"));
        assertTrue(query.get().getChildren().toString().contains("oneSchemaQuery"));

        final Optional<TypeDefinition> mutation = reg.getType("Mutation");
        assertTrue("Expecting Mutation", mutation.isPresent());
        assertTrue(mutation.get().getChildren().toString().contains("someMutation"));
    }

    @Test
    public void requires() throws Exception {
        final StringWriter target = new StringWriter();
        tracker.addingBundle(U.mockProviderBundle("SDL", 1, "a.sdl.txt", "b.sdl.txt", "c.sdl.txt"), null);
        dsa.aggregate(target, "c.sdl");
        final String sdl = target.toString();

        // Verify that required partials are included
        Stream.of(
            "someMutation",
            "typeFromB",
            "typeFromA"
        ).forEach((s -> {
            assertTrue("Expecting aggregate to contain " + s, sdl.contains(s));
        }));
   }

    @Test
    public void cycleInRequirements() throws Exception {
        final StringWriter target = new StringWriter();
        tracker.addingBundle(U.mockProviderBundle("SDL", 1, "circularA.txt", "circularB.txt"), null);
        final RuntimeException rex = assertThrows(RuntimeException.class, () -> dsa.aggregate(target, "circularA"));

        Stream.of(
            "requirements cycle",
            "circularA"
        ).forEach((s -> {
            assertTrue(String.format("Expecting message to contain %s: %s",  s, rex.getMessage()), rex.getMessage().contains(s));
        }));
    }

    @Test
    public void providersOrdering() throws Exception {
        final StringWriter target = new StringWriter();
        tracker.addingBundle(U.mockProviderBundle("ordering", 1, "Aprov.txt", "Cprov.txt", "Z_test.txt", "A_test.txt", "Zprov.txt", "Z_test.txt", "Bprov.txt", "C_test.txt"), null);
        dsa.aggregate(target, "Aprov", "Zprov", "/[A-Z]_test/", "A_test", "Cprov");
        final String sdl = target.toString();

        // The order of named partials is kept, regexp selected ones are ordered by name
        // And A_test has already been used so it's not used again when called explicitly after regexp
        final String expected = "End of Schema aggregated from {Aprov,Zprov,A_test,C_test,Z_test,Cprov} by DefaultSchemaAggregator";
        assertTrue(String.format("Expecting schema to contain [%s]: %s", expected, sdl), sdl.contains(expected));
   }
}