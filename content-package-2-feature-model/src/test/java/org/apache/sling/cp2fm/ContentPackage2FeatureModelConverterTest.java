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

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;

import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ContentPackage2FeatureModelConverterTest {

    private ContentPackage2FeatureModelConverter converter;

    @Before
    public void setUp() {
        converter = new ContentPackage2FeatureModelConverter();
    }

    @After
    public void tearDowd() {
        converter = null;
    }

    @Test(expected = NullPointerException.class)
    public void convertRequiresNonNullPackage() throws Exception {
        converter.convert(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertRequiresExistingFile() throws Exception {
        converter.convert(new File("this/file/does/not/exist.zip"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertRequiresNotDirectoryFile() throws Exception {
        File testDirectory = new File(System.getProperty("user.dir"));
        converter.convert(testDirectory);
    }

    @Test(expected = IllegalStateException.class)
    public void getRunModeRequiresConvertInvoked() {
        converter.getRunMode(null);
    }

    @Test(expected = IllegalStateException.class)
    public void addConfigurationRequiresConvertInvoked() {
        converter.setMergeConfigurations(true).getRunMode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void processRequiresNotNullPackage() throws Exception {
        converter.process(null);
    }

    @Test(expected = IllegalStateException.class)
    public void processRequiresConvertInvoked() throws Exception {
        converter.process(mock(VaultPackage.class));
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullInput() throws Exception {
        converter.deployLocallyAndAttach(null, null, null, null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullGroupId() throws Exception {
        converter.deployLocallyAndAttach(null, mock(InputStream.class), null, null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullArtifactId() throws Exception {
        converter.deployLocallyAndAttach(null, mock(InputStream.class), "org.apache.sling", null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullVersion() throws Exception {
        converter.deployLocallyAndAttach(null, mock(InputStream.class), "org.apache.sling", "org.apache.sling.cm2fm", null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullType() throws Exception {
        converter.deployLocallyAndAttach(null, mock(InputStream.class), "org.apache.sling", "org.apache.sling.cm2fm", "0.0.1", null, null);
    }

}
