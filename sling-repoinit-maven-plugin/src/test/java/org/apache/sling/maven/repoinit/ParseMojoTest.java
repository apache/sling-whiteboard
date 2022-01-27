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
import java.util.Collections;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParseMojoTest {

    ParseMojo parseMojo;

    CapturingLogger log = new CapturingLogger();

    @BeforeEach
    void beforeEach() throws IllegalArgumentException {
        parseMojo = new ParseMojo();
        log.reset();
        parseMojo.setLog(new DefaultLog(log));

    }

    @Test
    void testParser() throws MojoExecutionException {
        parseMojo.includedFiles = Collections.singletonList("*.txt");
        parseMojo.scriptBaseDir = new File("src/test/repoinit");

        parseMojo.execute();

        assertEquals(2, log.getLines().stream().filter(l -> l.contains("[info] Parsing script:")).count(),
                "Did not find the expected number of files parsed");

        assertTrue(log.getLogs().contains("[info] All scripts parsed successfully!"),
                () -> "Expected to find success message and did not in: \n" + log.getLogs());
    }

    @Test
    void testLargeFile() throws MojoExecutionException {
        parseMojo.includedFiles = Collections.singletonList("large/combined.txt");
        parseMojo.scriptBaseDir = new File("src/test/repoinit");

        parseMojo.execute();

        assertTrue(log.getLogs().contains("[info] All scripts parsed successfully!"),
                () -> "Expected to find success message and did not in: \n" + log.getLogs());
    }

    @Test
    void testNoFiles() throws MojoExecutionException {
        parseMojo.includedFiles = Collections.singletonList("*.tx");
        parseMojo.scriptBaseDir = new File("src/test/repoinit");

        parseMojo.execute();

        assertTrue(log.getLogs().contains("[warn] No files found in directory:"),
                () -> "Expected to warning and did not in: \n" + log.getLogs());

    }

    @ParameterizedTest
    @CsvSource({
            "invalid/invalid.txt,1.6.10,Failed to parse script",
            "invalid/apache.png,1.6.10,Failed to parse script" })
    void testFailures(String filePattern, String parserVersion, String messageSubStr)
            throws MojoExecutionException {
        parseMojo.includedFiles = Collections.singletonList(filePattern);
        parseMojo.scriptBaseDir = new File("src/test/repoinit");

        String message = assertThrows(MojoExecutionException.class, () -> parseMojo.execute()).getMessage();
        assertTrue(message.contains(messageSubStr), "Did not recieve expected message in: " + message);
    }
}