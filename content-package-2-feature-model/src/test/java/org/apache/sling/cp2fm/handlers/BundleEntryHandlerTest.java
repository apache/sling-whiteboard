/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.cp2fm.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter;
import org.apache.sling.cp2fm.DefaultBundlesDeployer;
import org.apache.sling.cp2fm.spi.EntryHandler;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(Parameterized.class)
public final class BundleEntryHandlerTest {

    private final String bundleLocation;

    private final EntryHandler bundleEntryHandler;

    public BundleEntryHandlerTest(String bundleLocation, EntryHandler bundleEntryHandler) {
        this.bundleLocation = bundleLocation;
        this.bundleEntryHandler = bundleEntryHandler;
    }

    @Test
    public void doesNotMatch() {
        assertFalse(bundleEntryHandler.matches("jcr_root/not/a/valid/recognised/bundle.jar"));
    }

    @Test
    public void matches() {
        assertTrue(bundleEntryHandler.matches(bundleLocation));
    }

    @Test
    public void deployBundle() throws Exception {
        Archive archive = mock(Archive.class);
        Entry entry = mock(Entry.class);

        when(entry.getName()).thenReturn("test-framework.jar");
        when(archive.openInputStream(entry)).then(new Answer<InputStream>() {

            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable {
                return getClass().getResourceAsStream(bundleLocation);
            }

        });

        ContentPackage2FeatureModelConverter converter = spy(ContentPackage2FeatureModelConverter.class);

        File testDirectory = new File(System.getProperty("testDirectory"), getClass().getName() + '_' + System.currentTimeMillis());
        when(converter.getOutputDirectory()).thenReturn(testDirectory);

        doCallRealMethod().when(converter).attach(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        when(converter.getArtifactDeployer()).thenReturn(new DefaultBundlesDeployer(testDirectory));

        Feature feature = new Feature(new ArtifactId("org.apache.sling", "org.apache.sling.cp2fm", "0.0.1", null, null));
        when(converter.getTargetFeature()).thenReturn(feature);
        when(converter.getRunMode(anyString())).thenReturn(feature);

        bundleEntryHandler.handle(bundleLocation, archive, entry, converter);

        assertTrue(new File(testDirectory, "bundles/org/apache/felix/org.apache.felix.framework/6.0.1/org.apache.felix.framework-6.0.1.pom").exists());
        assertTrue(new File(testDirectory, "bundles/org/apache/felix/org.apache.felix.framework/6.0.1/org.apache.felix.framework-6.0.1.jar").exists());

        assertFalse(converter.getTargetFeature().getBundles().isEmpty());
        assertEquals(1, feature.getBundles().size());
        assertEquals("org.apache.felix:org.apache.felix.framework:6.0.1", feature.getBundles().get(0).getId().toMvnId());
    }

    @Parameters
    public static Collection<Object[]> data() {
        final BundleEntryHandler bundleEntryHandler = new BundleEntryHandler();

        return Arrays.asList(new Object[][] {
            { "jcr_root/apps/asd/install/test-framework-no-pom.jar", bundleEntryHandler },
            { "jcr_root/apps/asd/install/test-framework.jar", bundleEntryHandler },
            { "jcr_root/apps/asd/install.author/test-framework.jar", bundleEntryHandler },
            { "jcr_root/apps/asd/install.publish/test-framework.jar", bundleEntryHandler }
        });
    }

}
