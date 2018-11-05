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
package org.apache.sling.capabilities.oakdescriptors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Repository;

import org.apache.sling.capabilities.CapabilitiesSource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class OakDescriptorSourceTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    private CapabilitiesSource source;

    private Dictionary<String, Object> asProperties(String... keyWhitelist) {
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("keyWhitelist", Arrays.asList(keyWhitelist));
        return props;
    }

    private void registerOakDescriptorSource(String... keyWhitelist) throws IOException {
        final ConfigurationAdmin ca = context.getService(ConfigurationAdmin.class);
        assertNotNull("Expecting a ConfigurationAdmin service", ca);
        final Configuration cfg = ca.getConfiguration(OakDescriptorSource.class.getName());
        cfg.update(asProperties(keyWhitelist));

        final OakDescriptorSource ss = new OakDescriptorSource();
        context.registerInjectActivateService(ss);

        source = context.getService(CapabilitiesSource.class);
        assertNotNull("Expecting our OakDescriptorSource to be registered", source);
        assertEquals("Expecting the OakDescriptorSource namespace", OakDescriptorSource.NAMESPACE,
                source.getNamespace());
    }

    @Test
    public void testEmptyWhitelist() throws IOException {
        registerOakDescriptorSource();
    }

    @Test
    public void testNull() throws Exception {
        registerOakDescriptorSource();
        assertNotNull(source.getCapabilities());
        assertNull(source.getCapabilities().get("foo"));
    }

    @Test
    public void testDefaultJcrDescriptors_listed() throws Exception {
        registerOakDescriptorSource(Repository.REP_NAME_DESC, Repository.REP_VENDOR_DESC);
        assertNotNull(source.getCapabilities());
        assertEquals("Apache Jackrabbit Oak", source.getCapabilities().get(Repository.REP_NAME_DESC));
        assertEquals("The Apache Software Foundation", source.getCapabilities().get(Repository.REP_VENDOR_DESC));
    }

    @Test
    public void testDefaultJcrDescriptors_filtered() throws Exception {
        registerOakDescriptorSource(Repository.REP_NAME_DESC);
        assertNotNull(source.getCapabilities());
        assertEquals("Apache Jackrabbit Oak", source.getCapabilities().get(Repository.REP_NAME_DESC));
        // Repository.REP_VENDOR_DESC is not exposed, so should return null
        assertNull(source.getCapabilities().get(Repository.REP_VENDOR_DESC));
    }
}
