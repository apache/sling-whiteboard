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
package org.apache.sling.cp2fm.vltpkg;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.impl.PackageManagerImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class VaultPackageAssemblerTest {

    private File testDirectory;

    private final VaultPackageAssembler assembler;

    private final String resourceLocation;

    public VaultPackageAssemblerTest(String resourceLocation, VaultPackageAssembler assembler) {
        this.resourceLocation = resourceLocation;
        this.assembler = assembler;
    }

    @Before
    public void setUp() {
        testDirectory = new File(System.getProperty("testDirectory"), getClass().getName() + '_' + System.currentTimeMillis());
    }

    @Test
    public void matchAll() {
        assembler.matches(resourceLocation);
    }

    @Test
    public void packageResource() throws Exception {
        assembler.addEntry(resourceLocation, getClass().getResourceAsStream("../handlers/" + resourceLocation));
        File contentPackage = assembler.createPackage(testDirectory);

        ZipFile zipFile = new ZipFile(contentPackage);
        ZipEntry entry = zipFile.getEntry(resourceLocation);
        assertNotNull(entry);
        zipFile.close();
    }

    @Parameters
    public static Collection<Object[]> data() throws Exception {
        URL resource = VaultPackageAssemblerTest.class.getResource("../test-content-package.zip");
        File file = FileUtils.toFile(resource);
        VaultPackage vaultPackage = new PackageManagerImpl().open(file);

        VaultPackageAssembler assembler = VaultPackageAssembler.create(vaultPackage);

        return Arrays.asList(new Object[][] {
            { "jcr_root/.content.xml", assembler },
            { "jcr_root/asd/.content.xml", assembler },
            { "jcr_root/asd/public/_rep_policy.xml", assembler },
            { "jcr_root/asd/public/license.txt", assembler }
        });
    }

}
