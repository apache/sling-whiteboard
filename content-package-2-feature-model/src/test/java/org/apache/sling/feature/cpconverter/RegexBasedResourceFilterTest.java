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
package org.apache.sling.feature.cpconverter;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RegexBasedResourceFilterTest {

    private RegexBasedResourceFilter filter;

    @Before
    public void setUp() {
        filter = new RegexBasedResourceFilter();
    }

    @After
    public void tearDown() {
        filter = null;
    }

    @Test
    public void packagesFilteredIn() {
        filter.addFilteringPattern(".*\\/myEnvironment(?!(\\.runMode1\\/|\\.runMode2\\/|\\/))(.*)(?=\\.zip$).*");

        assertFalse(filter.isFilteredOut("/apps/myapp/myEnvironment/something.zip"));
        assertFalse(filter.isFilteredOut("/apps/myapp/myEnvironment.runMode1/something.zip"));
        assertFalse(filter.isFilteredOut("/apps/myapp/myEnvironment.runMode2/something.zip"));

        assertFalse(filter.isFilteredOut("/apps/myEnvironment/asd.zip"));
    }

    @Test
    public void configFilteredOut() {
        filter.addFilteringPattern(".*\\/myEnvironment(?!(\\.runMode1\\/|\\.runMode2\\/|\\/))(.*)(?=\\.config$).*");

        assertTrue(filter.isFilteredOut("/apps/myapp/myEnvironment.subRunMode/something.config"));
        assertTrue(filter.isFilteredOut("/apps/myapp/myEnvironment.runMode1.subRunMode/something.config"));
        assertTrue(filter.isFilteredOut("/apps/myapp/myEnvironment.subRunMode.runMode1/something.config"));
    }

    @Test
    public void packagesFilteredOut() {
        filter.addFilteringPattern(".*\\/myEnvironment(?!(\\.runMode1\\/|\\.runMode2\\/|\\/))(.*)(?=\\.zip$).*");

        assertTrue(filter.isFilteredOut("/apps/myapp/myEnvironment.subRunMode/something.zip"));
        assertTrue(filter.isFilteredOut("/apps/myapp/myEnvironment.runMode1.subRunMode/something.zip"));
        assertTrue(filter.isFilteredOut("/apps/myapp/myEnvironment.subRunMode.runMode1/something.zip"));
        assertTrue(filter.isFilteredOut("/apps/myapp/myEnvironment.xyz/something.zip"));
    }

}
