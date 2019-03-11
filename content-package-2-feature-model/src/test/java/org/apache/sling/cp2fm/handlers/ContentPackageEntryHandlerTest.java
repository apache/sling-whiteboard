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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.cp2fm.spi.EntryHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class ContentPackageEntryHandlerTest {

    private EntryHandler contentPackageEntryhandler;

    @Before
    public void setUp() {
        contentPackageEntryhandler = new ContentPackageEntryHandler();
    }

    @After
    public void tearDown() {
        contentPackageEntryhandler = null;
    }

    @Test
    public void doesNotMatch() {
        assertFalse(contentPackageEntryhandler.matches("/this/is/a/path/not/pointing/to/a/valid/configuration.asd"));
    }

    @Test
    public void matches() {
        assertTrue(contentPackageEntryhandler.matches("jcr_root/etc/packages/asd/v6/sample/asd.content-1.0.zip"));
    }

}
