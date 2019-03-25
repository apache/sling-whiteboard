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
package org.apache.sling.cli.impl.release;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ReleaseTest {

    @Test
    public void fromRepositoryDescription() {
        
        Release rel1 = Release.fromString("Apache Sling Resource Merger 1.3.10 RC1");
        Release rel2 = Release.fromString("   Apache Sling Resource Merger    1.3.10   ");

        assertEquals("Resource Merger 1.3.10", rel1.getName());
        assertEquals("Apache Sling Resource Merger 1.3.10", rel1.getFullName());
        assertEquals("1.3.10", rel1.getVersion());
        assertEquals("Resource Merger", rel1.getComponent());

        assertEquals(rel1, rel2);
    }

    @Test
    public void testReleaseParsingWithJIRAInfo() throws URISyntaxException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(getClass().getResource("/jira_versions.txt").toURI())));
        reader.lines().forEach(line -> {
            if (!line.startsWith("#") && !"".equals(line)) {
                Release jiraRelease = Release.fromString(line);
                String releaseFullName = jiraRelease.getFullName();
                if (releaseFullName == null) {
                    fail("Failed to parse JIRA version: " + line);
                }
                int indexComponent = line.indexOf(jiraRelease.getComponent());
                int indexVersion = line.indexOf(jiraRelease.getVersion());
                assertTrue(indexComponent >= 0 && indexVersion > indexComponent);
            }
        });
        reader.close();
    }


}
