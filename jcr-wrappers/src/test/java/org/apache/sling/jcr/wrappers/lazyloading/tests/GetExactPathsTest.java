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

import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Verify that content is loaded on demand if we access the paths
 *  for which we have content directly.
 */
public class GetExactPathsTest extends TestBase {
    
    /** Use a single Repository instance for all our tests, to be able
     *  to check parent paths after loading specific Nodes
     */
    @BeforeAll
    public static void staticSetup() throws Exception {
        setupRepository();
    }

    @Override
    protected void beforeEachTest() {
    }

    @MethodSource
    static Stream<String> testPathsProvider() {
        // Verify that our exact test content paths are found
        // with Session.nodeExists, and then add a few of their parent
        // paths to verify that content has been saved
        return Stream.concat(
            Stream.of(TEST_CONTENT_PATHS), 
            Stream.of(
                "/content/slingshot/lazy/docs/content-model.html",
                "/content/starter/lazy/gradient.png",
                "/content/starter/lazy/img/asf-logo.svg",
                "/content/starter",
                "/content"
            )
        );
    }
    
    @ParameterizedTest
    @MethodSource("testPathsProvider")
    public void pathExists(String path) throws RepositoryException {
        assertTrue(session.nodeExists(path), "Expecting path to be found:" + path);
    }
}