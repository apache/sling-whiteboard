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
package org.apache.sling.maven.repoinit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.sling.maven.repoinit.ContentFolder.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ValidateMojoTest {

    ValidateMojo validateMojo;

    CapturingLogger log = new CapturingLogger();

    @BeforeEach
    void beforeEach() throws IllegalArgumentException {
        validateMojo = new ValidateMojo();
        log.reset();
        validateMojo.setLog(new DefaultLog(log));
    }

    @Test
    void testValidator() throws MojoExecutionException {
        validateMojo.includedFiles = Collections.singletonList("*.txt");
        validateMojo.scriptBaseDir = new File("src/test/repoinit");

        validateMojo.execute();

        assertEquals(2, log.getLines().stream().filter(l -> l.contains("[debug] Parsing script:")).count(),
                "Did not find the expected number of files parsed");

        assertTrue(log.getLogs().contains("[info] All scripts executed successfully!"),
                () -> "Expected to find success message and did not in: \n" + log.getLogs());
    }

    @Test
    void testLoadCnd() throws MojoExecutionException {
        validateMojo.scriptBaseDir = new File("src/test/repoinit");
        validateMojo.includedFiles = Collections.singletonList("invalid/requires-cnd.txt");

        Throwable realCause = assertThrows(MojoExecutionException.class, () -> validateMojo.execute(),
                "Expected loading of repoinit script to fail due to missing node types").getCause().getCause();
        assertTrue(realCause instanceof NoSuchNodeTypeException, "Expected exception type not thrown");

        log.reset();
        validateMojo.nodeDefinitions = Collections.singletonList("src/test/resources/cnd/test.cnd");
        validateMojo.execute();
        assertTrue(log.getLogs().contains("[info] All scripts executed successfully!"),
                () -> "Expected to find success message and did not in: \n" + log.getLogs());
    }

    @Test
    void testLoadContent() throws MojoExecutionException {
        validateMojo.scriptBaseDir = new File("src/test/repoinit");
        validateMojo.includedFiles = Collections.singletonList("invalid/requires-cnd.txt");
        validateMojo.nodeDefinitions = Collections.singletonList("src/test/resources/cnd/test.cnd");

        ContentFolder contentFolder = new ContentFolder();
        contentFolder.setFolder(new File("src/test/resources/contentloader/json"));
        contentFolder.setType(Type.JSON);
        contentFolder.setPath("/etc/taxonomy");
        validateMojo.contentFolders = Collections.singletonList(contentFolder);
        validateMojo.execute();
        assertTrue(log.getLogs().contains("[info] All scripts executed successfully!"),
                () -> "Expected to find success message and did not in: \n" + log.getLogs());
    }

    @Test
    void testFailingScriptScript() throws MojoExecutionException {
        validateMojo.scriptBaseDir = new File("src/test/repoinit");
        validateMojo.includedFiles = Collections.singletonList("invalid/willfail.txt");
        validateMojo.nodeDefinitions = Collections.emptyList();

        validateMojo.contentFolders = Collections.emptyList();
        assertThrows(MojoExecutionException.class, () -> validateMojo.execute());
    }

    @ParameterizedTest
    @MethodSource
    void testInvalidContentFolder(ContentFolder contentFolder) throws MojoExecutionException {
        validateMojo.scriptBaseDir = new File("src/test/repoinit");
        validateMojo.includedFiles = Collections.singletonList("invalid/requires-cnd.txt");
        validateMojo.nodeDefinitions = Collections.singletonList("src/test/resources/cnd/test.cnd");

        validateMojo.contentFolders = Collections.singletonList(contentFolder);
        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> validateMojo.execute(),
                "Expected loading of repoinit script to fail due to missing node types");
        assertTrue(exception.getMessage().contains("Invalid initial content folder: ContentFolder"),
                "Unexpected message: " + exception.getMessage());

    }

    static Stream<ContentFolder> testInvalidContentFolder() {
        List<ContentFolder> invalidFolders = new ArrayList<>();
        invalidFolders.add(new ContentFolder(new File("src/test/resources/contentloader/json"), "/etc/taxonomy", null));
        invalidFolders.add(new ContentFolder(new File("src/test/resources/contentloader/json"), null, Type.JSON));
        invalidFolders
                .add(new ContentFolder(new File("src/test/resources/contentloader/not/a/folder"), "/", Type.JSON));
        invalidFolders
                .add(new ContentFolder(new File("src/test/resources/contentloader/json/tags.json"), "/", Type.JSON));
        return invalidFolders.stream();
    }

}