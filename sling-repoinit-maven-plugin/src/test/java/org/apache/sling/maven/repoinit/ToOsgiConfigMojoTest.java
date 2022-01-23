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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToOsgiConfigMojoTest {

    ToOsgiConfigMojo toOsgiConfigMojo;

    CapturingLogger log = new CapturingLogger();

    @BeforeEach
    void beforeEach() {
        toOsgiConfigMojo = new ToOsgiConfigMojo();
        log.reset();
        toOsgiConfigMojo.setLog(new DefaultLog(log));
    }

    @ParameterizedTest
    @ValueSource(strings = { "JSON", "CONFIG" })
    void testConverter(String outputFormat) throws MojoExecutionException {
        toOsgiConfigMojo.includedFiles = Collections.singletonList("*.txt");

        toOsgiConfigMojo.scriptBaseDir = new File("src/test/repoinit");
        toOsgiConfigMojo.outputFormat = outputFormat;
        toOsgiConfigMojo.outputDir = new File("target/unit-test-output");

        toOsgiConfigMojo.execute();

        assertEquals(2, log.getLines().stream().filter(l -> l.contains("[info] Converting script:")).count(),
                "Did not find the expected number of files converted");

        assertTrue(log.getLogs().contains("[info] All scripts converted successfully"),
                () -> "Expected to find success message and did not in: \n" + log.getLogs());
    }

    @Test
    void testLargeFile() throws MojoExecutionException {
        toOsgiConfigMojo.includedFiles = Collections.singletonList("large/combined.txt");
        toOsgiConfigMojo.scriptBaseDir = new File("src/test/repoinit");
        toOsgiConfigMojo.outputFormat = "JSON";
        toOsgiConfigMojo.outputDir = new File("target/unit-test-output");

        toOsgiConfigMojo.execute();

        assertTrue(log.getLogs().contains("[info] All scripts converted successfully!"),
                () -> "Expected to find success message and did not in: \n" + log.getLogs());
    }

    @Test
    void testNoFiles() throws MojoExecutionException {
        toOsgiConfigMojo.includedFiles = Collections.singletonList("*.tx");
        toOsgiConfigMojo.scriptBaseDir = new File("src/test/repoinit");
        toOsgiConfigMojo.outputFormat = "JSON";
        toOsgiConfigMojo.outputDir = new File("target/unit-test-output");

        toOsgiConfigMojo.execute();

        assertTrue(log.getLogs().contains("[warn] No scripts found in directory:"),
                () -> "Expected to warning and did not in: \n" + log.getLogs());
    }

    @Test
    void testInvalidBase()
            throws MojoExecutionException {
        toOsgiConfigMojo.includedFiles = Collections.singletonList("*.txt");
        toOsgiConfigMojo.outputFormat = "JSON";
        toOsgiConfigMojo.outputDir = new File("target/unit-test-output");

        toOsgiConfigMojo.scriptBaseDir = new File("src/test/repoinit/somethingelse");

        String message = assertThrows(MojoExecutionException.class, () -> toOsgiConfigMojo.execute()).getMessage();
        assertTrue(message.contains("Could not find scripts in directory:"),
                "Did not recieve expected message in: " + message);
    }

    @ParameterizedTest
    @CsvSource({ "invalid/apache.png,JSON,Failed to convert script",
            "*.txt,XML,Unsupported output format: XML" })
    void testFailures(String filePattern, String format, String messageSubStr)
            throws MojoExecutionException {
        toOsgiConfigMojo.includedFiles = Collections.singletonList(filePattern);
        toOsgiConfigMojo.outputFormat = format;
        toOsgiConfigMojo.outputDir = new File("target/unit-test-output");

        toOsgiConfigMojo.scriptBaseDir = new File("src/test/repoinit");

        String message = assertThrows(MojoExecutionException.class, () -> toOsgiConfigMojo.execute()).getMessage();
        assertTrue(message.contains(messageSubStr), "Did not recieve expected message in: " + message);
    }

}