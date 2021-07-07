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

import org.apache.sling.graphql.schema.aggregator.servlet.SchemaAggregatorServlet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;

public class SchemaAggregatorServletTest {

    private void assertMappings(Map<String, String[]> data, String selector, String expected) {
        final String [] names = data.get(selector);
        assertNotNull("Expecting field names for selector " + selector, names);
        assertEquals(expected, String.join(",", names));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void selectorMappingConfig() throws Exception {
        final SchemaAggregatorServlet s = new SchemaAggregatorServlet();
        final SchemaAggregatorServlet.Config cfg = mock(SchemaAggregatorServlet.Config.class);
        final String [] cfgMappings = {
            "\t S1\t :one, two,   \t three  \t",
            "selector_2:4,5"
        };
        when(cfg.selectors_to_partials_mapping()).thenReturn(cfgMappings);
        s.activate(null, cfg);
        final Field f = s.getClass().getDeclaredField("selectorsToPartialNames");
        f.setAccessible(true);
        final Map<String, String[]> actualMappings = (Map<String, String[]>)f.get(s);
        assertEquals(2, actualMappings.size());
        assertMappings(actualMappings, "S1", "one,two,three");
        assertMappings(actualMappings, "selector_2", "4,5");
    }
}