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
package org.apache.sling.graphql.schema.aggregator;

import org.osgi.framework.Bundle;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import org.apache.sling.graphql.schema.aggregator.impl.ProviderBundleTracker;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.osgi.framework.Constants;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/** Test Utilities */
public class U {
    
    public static Bundle mockProviderBundle(String symbolicName, long id, String ... schemaNames) throws IOException {
        final Bundle b = mock(Bundle.class);
        when(b.getSymbolicName()).thenReturn(symbolicName);
        when(b.getBundleId()).thenReturn(id);

        final Dictionary<String, String> headers = new Hashtable<>();
        String fakePath = symbolicName + "/path/" + id;
        headers.put(ProviderBundleTracker.SCHEMA_PATH_HEADER, fakePath);
        when(b.getHeaders()).thenReturn(headers);

        final List<String> resources = new ArrayList<>();
        for(String name : schemaNames) {
            String fakeResource = fakePath + "/resource/" + name;
            resources.add(fakeResource);
            when(b.getEntry(fakeResource)).thenReturn(fakePartialURL(name));
        }
        when(b.getEntryPaths(fakePath)).thenReturn(Collections.enumeration(resources));
        return b;
    }

    /** Simple way to get a URL: create a temp file */
    public static URL fakePartialURL(String name) throws IOException {
        final File f = File.createTempFile(name, "txt");
        f.deleteOnExit();
        final PrintWriter w = new PrintWriter(new FileWriter(f));
        w.print(fakePartialSchema(name));
        w.flush();
        w.close();
        // Safe in our case, we're using acceptable characters in the path
        return f.toURL();
    }

    public static String fakePartialSchema(String name) {
        return String.format("PARTIAL:%s\nQUERY:%s\nFake query for %s\n", name, name, name);
    }

    public static Option tinyProviderBundle(String symbolicName, String ... partialsNames) {
        final String schemaPath = symbolicName + "/schemas";
        final TinyBundle b = bundle()
            .set(ProviderBundleTracker.SCHEMA_PATH_HEADER, schemaPath)
            .set(Constants.BUNDLE_SYMBOLICNAME, symbolicName)
            .set(Constants.REQUIRE_CAPABILITY, "org.apache.sling.graphql.schema.aggregator;filter:=\"(syntax>=0.1)\"")
        ;

        for(String name : partialsNames) {
            final String resourcePath = schemaPath + "/" + name + ".txt";
            b.add(resourcePath, new ByteArrayInputStream(fakePartialSchema(name).getBytes()));
        }

        return streamBundle(b.build());
    }

    public static void assertPartialsFoundInSchema(String output, String ... partialName) {
        for(String name : partialName) {
            final String expected = "DefaultSchemaAggregator.source=" + name;
            if(!output.contains(expected)) {
                fail(String.format("Expecting output to contain %s: %s", expected, output));
            }
        }
    }
}