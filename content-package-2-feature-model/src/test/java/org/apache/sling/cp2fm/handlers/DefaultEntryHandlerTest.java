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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter;
import org.apache.sling.cp2fm.spi.EntryHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DefaultEntryHandlerTest {

    private final String resourceLocation;

    private final EntryHandler defaultEntryHandler;

    public DefaultEntryHandlerTest(String recourceLocation, EntryHandler defaultEntryHandler) {
        this.resourceLocation = recourceLocation;
        this.defaultEntryHandler = defaultEntryHandler;
    }

    @Test
    public void matchAll() {
        assertTrue(defaultEntryHandler.matches(resourceLocation));
    }

    @Test
    public void copyEverything() throws Exception {
        Archive archive = mock(Archive.class);
        Entry entry = mock(Entry.class);
        when(archive.openInputStream(entry)).thenReturn(getClass().getResourceAsStream(resourceLocation));

        ContentPackage2FeatureModelConverter converter = spy(ContentPackage2FeatureModelConverter.class);

        File testDirectory = new File(System.getProperty("testDirectory"), getClass().getName());
        when(converter.getOutputDirectory()).thenReturn(testDirectory);

        defaultEntryHandler.handle(resourceLocation, archive, entry, converter);

        File targetDirectory = new File(testDirectory, "tmp-deflated");
        File targetFile = new File(targetDirectory, resourceLocation);
        assertTrue(targetFile.exists());
    }

    @Parameters
    public static Collection<Object[]> data() {
        EntryHandler defaultEntryHandler = new DefaultEntryHandler();

        return Arrays.asList(new Object[][] {
            { "jcr_root/.content.xml", defaultEntryHandler },
            { "jcr_root/asd/.content.xml", defaultEntryHandler },
            { "jcr_root/asd/public/_rep_policy.xml", defaultEntryHandler },
            { "jcr_root/asd/public/license.txt", defaultEntryHandler }
        });
    }

}
