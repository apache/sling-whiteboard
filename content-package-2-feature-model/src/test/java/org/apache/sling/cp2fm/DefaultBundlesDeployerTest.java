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
package org.apache.sling.cp2fm;

import static org.junit.Assert.*;

import static org.mockito.Mockito.mock;

import java.io.File;

import org.apache.sling.cp2fm.spi.BundlesDeployer;
import org.apache.sling.cp2fm.spi.ArtifactWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultBundlesDeployerTest {

    private BundlesDeployer artifactDeployer;

    @Before
    public void setUp() {
        File outputDirectory = new File(System.getProperty("testDirectory"), getClass().getName() + '_' + System.currentTimeMillis());
        artifactDeployer = new DefaultBundlesDeployer(outputDirectory);
    }

    @After
    public void tearDown() {
        artifactDeployer = null;
    }

    @Test
    public void verifyBundlesDirectory() {
        File bundlesDirectory = artifactDeployer.getBundlesDirectory();
        assertNotNull(bundlesDirectory);
        assertTrue(bundlesDirectory.exists());
        assertTrue(bundlesDirectory.isDirectory());
        assertEquals("bundles", bundlesDirectory.getName());
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullInput() throws Exception {
        artifactDeployer.deploy(null, null, null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullGroupId() throws Exception {
        artifactDeployer.deploy(mock(ArtifactWriter.class), null, null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullArtifactId() throws Exception {
        artifactDeployer.deploy(mock(ArtifactWriter.class), "org.apache.sling", null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullVersion() throws Exception {
        artifactDeployer.deploy(mock(ArtifactWriter.class), "org.apache.sling", "org.apache.sling.cm2fm", null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullType() throws Exception {
        artifactDeployer.deploy(mock(ArtifactWriter.class), "org.apache.sling", "org.apache.sling.cm2fm", "0.0.1", null, null);
    }

}
