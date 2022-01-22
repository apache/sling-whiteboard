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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ParseMojoTest {

    @Mock
    RepositorySystem repoSystem;

    @Mock
    RepositorySystemSession repoSession;

    ParseMojo parseMojo;

    CapturingLogger log = new CapturingLogger();

    @BeforeEach
    public void beforeEach() throws IllegalArgumentException, ArtifactResolutionException {
        parseMojo = new ParseMojo();
        parseMojo.repoSystem = repoSystem;
        parseMojo.repoSession = repoSession;
        log.reset();
        parseMojo.setLog(new DefaultLog(log));

        when(repoSystem.resolveArtifact(any(RepositorySystemSession.class), any(ArtifactRequest.class)))
                .thenAnswer(this::mockResponse);
    }

    private ArtifactResult mockResponse(InvocationOnMock invocation) throws ArtifactResolutionException {
        ArtifactRequest request = (ArtifactRequest) invocation.getArguments()[1];

        File artifactFile = new File(
                "target/unit-test-jars/org.apache.sling.repoinit.parser-" + request.getArtifact().getVersion()
                        + ".jar");
        ArtifactResult result = new ArtifactResult(request);
        if (!artifactFile.exists()) {
            throw new ArtifactResolutionException(Collections.singletonList(result));
        }
        Artifact artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(artifactFile);

        result.setArtifact(artifact);
        return result;
    }

    @Test
    public void testParser() throws MojoExecutionException {
        parseMojo.includedFiles = Collections.singletonList("*.txt");
        parseMojo.parserVersion = "1.2.0";
        parseMojo.scriptBaseDir = new File("src/test/repoinit");

        parseMojo.execute();

        assertEquals(2, log.getLines().stream().filter(l -> l.contains("[info] Parsing script:")).count(),
                "Did not find the expected number of files parsed");

        assertTrue(log.getLogs().contains("[info] All scripts parsed successfully!"),
                () -> "Expected to find success message and did not in: \n" + log.getLogs());
    }

    @Test
    public void testLargeFile() throws MojoExecutionException {
        parseMojo.includedFiles = Collections.singletonList("large/combined.txt");
        parseMojo.parserVersion = "1.6.10";
        parseMojo.scriptBaseDir = new File("src/test/repoinit");

        parseMojo.execute();

        assertTrue(log.getLogs().contains("[info] All scripts parsed successfully!"),
                () -> "Expected to find success message and did not in: \n" + log.getLogs());
    }

    @Test
    public void testNoFiles() throws MojoExecutionException {
        parseMojo.includedFiles = Collections.singletonList("*.tx");
        parseMojo.parserVersion = "1.2.0";
        parseMojo.scriptBaseDir = new File("src/test/repoinit");

        parseMojo.execute();

        assertTrue(log.getLogs().contains("[warn] No scripts found in directory:"),
                () -> "Expected to warning and did not in: \n" + log.getLogs());

        assertFalse(log.getLogs().contains("Getting Repoint Parser version"),
                () -> "Expected not to resolve repoinit parser version if no scripts found");
    }

    @ParameterizedTest
    @CsvSource({
            "invalid/unsupported.txt,1.2.0,Failed to parse script",
            "invalid/invalid.txt,1.6.10,Failed to parse script",
            "invalid/apache.png,1.6.10,Failed to parse script",
            "*.txt,0.2.0,Couldn't download artifact: The following artifacts could not be resolved: org.apache.sling:org.apache.sling.repoinit.parser:jar:0.2.0" })
    public void testFailures(String filePattern, String parserVersion, String messageSubStr)
            throws MojoExecutionException {
        parseMojo.includedFiles = Collections.singletonList(filePattern);
        parseMojo.parserVersion = parserVersion;
        parseMojo.scriptBaseDir = new File("src/test/repoinit");

        String message = assertThrows(MojoExecutionException.class, () -> parseMojo.execute()).getMessage();
        assertTrue(message.contains(messageSubStr), "Did not recieve expected message in: " + message);
    }
}