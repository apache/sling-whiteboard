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
package org.apache.sling.jcr.wrappers.lazyloading.tests;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.RepositoryException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class VisitSlashContentTest extends TestBase {
    
    @Test
    public void recursiveVisitTest() throws RepositoryException {
        final Set<String> allPathsFound = new HashSet<>();
        
        assertNoContent();

        // Verify that a recursive visit of /content, which does not exist
        // at this point, causes our test content to be loaded
        visitRecursively(session.getRootNode(), p -> p.startsWith("/content"), allPathsFound);
        
        for(String path : TEST_CONTENT_PATHS) {
            assertTrue(allPathsFound.contains(path), "Expecting content path to be found:" + path);
        }

        for(String path : Arrays.asList("/content/starter", "/content/slingshot/lazy/docs/content-model.html")) {
            assertTrue(allPathsFound.contains(path), "Expecting parent path to be found:" + path);
        }
    }
}