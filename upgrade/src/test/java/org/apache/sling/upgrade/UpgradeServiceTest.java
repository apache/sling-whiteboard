/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.upgrade.impl.BundleEntryFactory;
import org.apache.sling.upgrade.impl.ConfigEntryFactory;
import org.apache.sling.upgrade.impl.StartupBundleEntryFactory;
import org.apache.sling.upgrade.impl.UpgradeServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeServiceTest {

    private static final Logger log = LoggerFactory.getLogger(UpgradeServiceTest.class);

    private InputStream jar;

    private UpgradeService upgradeService;

    private UpgradeRequest request;

    @Before
    public void init() throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        jar = getClass().getClassLoader().getResourceAsStream("sling.jar");
        upgradeService = new UpgradeServiceImpl();

        // Initialize a fake OSGi Container
        ComponentContext componentContext = Mockito.mock(ComponentContext.class);
        BundleContext bundleContext = Mockito.mock(BundleContext.class);
        Mockito.when(componentContext.getBundleContext()).thenReturn(bundleContext);

        // Provide some bundles
        Bundle bundle1 = Mockito.mock(Bundle.class);
        Mockito.when(bundle1.getVersion()).thenReturn(new Version("2.4.0"));
        Mockito.when(bundle1.getSymbolicName()).thenReturn("org.apache.sling.jcr.api");

        Bundle bundle2 = Mockito.mock(Bundle.class);
        Mockito.when(bundle2.getVersion()).thenReturn(new Version("3.0.0"));
        Mockito.when(bundle2.getSymbolicName()).thenReturn("org.apache.sling.jcr.base");

        Bundle bundle3 = Mockito.mock(Bundle.class);
        Mockito.when(bundle3.getVersion()).thenReturn(new Version("4.0.0"));
        Mockito.when(bundle3.getSymbolicName()).thenReturn("org.apache.sling.jcr.davex");

        Bundle[] bundles = new Bundle[] { bundle1, bundle2, bundle3 };
        Mockito.when(bundleContext.getBundles()).thenReturn(bundles);

        BundleEntryFactory bef = new BundleEntryFactory();
        bef.activate(componentContext);
        Field settingsService = bef.getClass().getDeclaredField("settingsService");
        SlingSettingsService sso = Mockito.mock(SlingSettingsService.class);
        Set<String> runmodes = new HashSet<>();
        runmodes.add("oak_tar");
        Mockito.when(sso.getRunModes()).thenReturn(runmodes);
        settingsService.setAccessible(true);
        settingsService.set(bef, sso);

        StartupBundleEntryFactory sbef = new StartupBundleEntryFactory();
        sbef.activate(componentContext);

        List<EntryHandlerFactory<?>> factories = new ArrayList<>();
        factories.add(bef);
        factories.add(sbef);
        factories.add(new ConfigEntryFactory());

        Field field = upgradeService.getClass().getDeclaredField("entryFactories");
        field.setAccessible(true);
        field.set(upgradeService, factories);

        // read the request
        this.request = upgradeService.readSlingJar(jar);

    }

    @Test
    public void testUpgradeRequest() throws IOException {
        log.info("testUpgradeRequest");

        assertEquals("Apache Sling - CMS Application Builder", request.getTitle());
        assertEquals("The Apache Software Foundation", request.getVendor());
        assertEquals("0.10.1-SNAPSHOT", request.getVersion());

        log.info("Test successful!");
    }

    @Test
    public void testUpgradeRequestBundles() throws IOException {
        log.info("testUpgradeRequestBundles");
        List<BundleEntry> bundles = request.getEntriesByType(BundleEntry.class);
        assertNotNull(bundles);

        assertTrue(!bundles.isEmpty());

        for (BundleEntry bundle : bundles) {
            assertNotNull(bundle.getContents());
            assertNotNull(bundle.getStart());
            assertNotNull(bundle.getSymbolicName());
            assertNotNull(bundle.getVersion());
            assertFalse("oak_mongo".equals(bundle.getRunmode()));
            switch (bundle.getSymbolicName()) {
            case "org.apache.sling.jcr.api":
                assertTrue(bundle.isInstalled());
                assertFalse(bundle.isUpdated());
                break;
            case "org.apache.sling.jcr.base":
                assertTrue(bundle.isInstalled());
                assertTrue(bundle.isUpdated());
                break;
            case "org.apache.sling.jcr.davex":
                assertTrue(bundle.isInstalled());
                assertFalse(bundle.isUpdated());
                break;
            default:
                assertTrue(bundle.isUpdated());
                assertFalse(bundle.isInstalled());
                break;
            }
            log.debug("Bundle: " + bundle.getSymbolicName());
        }

        log.info("Test successful!");
    }

    @Test
    public void testUpgradeRequestConfigs() throws IOException {
        log.info("testUpgradeRequestConfigs");
        List<ConfigEntry> configs = request.getEntriesByType(ConfigEntry.class);
        assertNotNull(configs);

        assertTrue(!configs.isEmpty());

        configs.forEach(c -> log.debug("Config: " + c.getPid()));

        log.info("Test successful!");
    }
}
