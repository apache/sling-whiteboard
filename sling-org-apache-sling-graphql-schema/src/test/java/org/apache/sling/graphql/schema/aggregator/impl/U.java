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

import org.osgi.framework.Bundle;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

class U {
    static Bundle testBundle(String symbolicName, long id, int nEntries) {
        final Bundle b = mock(Bundle.class);
        when(b.getSymbolicName()).thenReturn(symbolicName);
        when(b.getBundleId()).thenReturn(id);

        final Dictionary<String, String> headers = new Hashtable<>();
        String fakePath = symbolicName + "/path/" + id;
        headers.put(ProviderBundleTracker.SCHEMA_PATH_HEADER, fakePath);
        when(b.getHeaders()).thenReturn(headers);

        final List<String> resources = new ArrayList<>();
        for(int i=1 ; i <= nEntries; i++) {
            String fakeResource = fakePath + "/resource/" + i;
            resources.add(fakeResource);
            final URL url = mock(URL.class);
            when(url.toString()).thenReturn(fakeResource);
            when(b.getEntry(fakeResource)).thenReturn(url);
        }
        when(b.getEntryPaths(fakePath)).thenReturn(Collections.enumeration(resources));
        return b;
    }
}