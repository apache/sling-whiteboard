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

package org.apache.sling.apiplanes;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.apiplanes.prototype.ExtensionOverrideRequestWrapper;

public class ExtensionOverrideRequestWrapperTest {

    @Test
    public void testWrapping() {
        final SlingHttpServletRequest r = mock(SlingHttpServletRequest.class);
        final RequestPathInfo originalRpi = mock(RequestPathInfo.class);
        when(r.getRequestPathInfo()).thenReturn(originalRpi);
        when(originalRpi.getExtension()).thenReturn("e_orig");
        when(originalRpi.getSelectors()).thenReturn(new String [] { "s1", "s2", "s3"} );
        when(originalRpi.getSelectorString()).thenReturn("s1.s2.s3");

        final ExtensionOverrideRequestWrapper w = new ExtensionOverrideRequestWrapper(r, "e_new");
        final RequestPathInfo wRpi = w.getRequestPathInfo();
        assertEquals("e_new", wRpi.getExtension());
        assertEquals("s1.s2.s3.e_orig", wRpi.getSelectorString());
        assertArrayEquals(new String [] { "s1", "s2", "s3", "e_orig" }, wRpi.getSelectors());
    }
}
