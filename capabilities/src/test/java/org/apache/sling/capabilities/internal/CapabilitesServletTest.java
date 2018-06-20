/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.capabilities.internal;

import java.io.IOException;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.capabilities.CapabilitiesSource;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.sling.MockSling;
import static org.junit.Assert.assertEquals;
import org.osgi.framework.BundleContext;

/** Test the JSONCapabilitiesWriter */
public class CapabilitesServletTest {

    private SlingSafeMethodsServlet servlet;

    @Rule
    public final OsgiContext context = new OsgiContext();
    
    private BundleContext bundleContext;
    
    @Before
    public void setup() {
        servlet = new CapabilitiesServlet();
        bundleContext = MockOsgi.newBundleContext();
        
        bundleContext.registerService(CapabilitiesSource.class.getName(), new MockSource("F", 2), null);
        bundleContext.registerService(CapabilitiesSource.class.getName(), new MockSource("G", 43), null);
        
        MockOsgi.injectServices(servlet, bundleContext);
    }
    
    @Test
    public void testServlet() throws ServletException, IOException {
        final ResourceResolver resolver = MockSling.newResourceResolver(bundleContext);
        MockSlingHttpServletRequest req = new MockSlingHttpServletRequest(resolver);
        MockSlingHttpServletResponse resp = new MockSlingHttpServletResponse();
        
        servlet.service(req, resp);

        // Just verify that both sources are taken into account
        // the JSON format details are tested elsewhere
        final JsonReader r = Json.createReader(new StringReader(resp.getOutputAsString()));
        final JsonObject rootJson = r.readObject();
        final JsonObject json = rootJson.getJsonObject(JSONCapabilitiesWriter.CAPS_KEY);
        assertEquals("VALUE_1_F", json.getJsonObject("F").getString("KEY_1_F"));
        assertEquals("VALUE_42_G", json.getJsonObject("G").getString("KEY_42_G"));
    }
}