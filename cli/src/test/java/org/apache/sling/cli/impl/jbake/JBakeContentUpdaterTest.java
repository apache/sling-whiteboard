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
package org.apache.sling.cli.impl.jbake;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffEntry.Side;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JBakeContentUpdaterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    
    private JBakeContentUpdater updater;
    
    @Before
    public void setUp() throws IOException {
        
        updater = new JBakeContentUpdater();
        // copy the file away so we don't modify what is in source control
        Files.copy(getClass().getResourceAsStream("/downloads.tpl"), Paths.get(new File(tmp.getRoot(), "downloads.tpl").toURI()));
        Files.copy(getClass().getResourceAsStream("/releases.md"), Paths.get(new File(tmp.getRoot(), "releases.md").toURI()));
    }
    
    @Test
    public void updateDownloadsTemplate_newReleaseOfExistingModule() throws IOException {
        
        updateDownloadsTemplate0("API", "2.20.2");
    }

    private void updateDownloadsTemplate0(String newReleaseName, String newReleaseVersion) throws IOException {
        Path templatePath = Paths.get(new File(tmp.getRoot(), "downloads.tpl").toURI());
        
        int changeCount = updater.updateDownloads(templatePath, newReleaseName, newReleaseVersion);
        assertThat("Unexpected count of changes", changeCount, equalTo(1));

        String apiLine = Files.readAllLines(templatePath, StandardCharsets.UTF_8).stream()
            .filter( l -> l.trim().startsWith("\"" + newReleaseName + "|"))
            .findFirst()
            .get();
        
        assertThat("Did not find modified version in the release line", apiLine, containsString(newReleaseVersion));
    }

    @Test
    public void updateDownloadsTemplate_newReleaseOfExistingMavenPlugin() throws IOException {
        
        updateDownloadsTemplate0("Slingstart Maven Plugin", "1.9.0");
    }

    @Test
    public void updateDownloadsTemplate_newReleaseOfIDETooling() throws IOException {

        updateDownloadsTemplate0("Sling IDE Tooling for Eclipse", "1.4.0");
    }
    
    @Test
    public void updateReleases_releaseInExistingMonth() throws IOException, GitAPIException {
        updateReleases0(LocalDateTime.of(2019, 2, 27, 22, 00), 
            Arrays.asList( 
                " " ,  
                " ## February 2019", 
                " " ,
                "+* API 2.20.2 (27th)",
                " * DataSource Provider 1.0.4, Resource Collection API 1.0.2, JCR ResourceResolver 3.0.18 (26th)",  
                " * Scripting JSP Tag Library 2.4.0, Scripting JSP Tag Library (Compat) 1.0.0 (18th)",
                " * Pipes 3.1.0 (15th)"
            )
        );
        
    }

    private void updateReleases0(LocalDateTime releaseDate, List<String> expectedLines, String... releaseNameAndInfo) throws IOException, GitAPIException {
        
        if ( releaseNameAndInfo.length > 2 )
            throw new IllegalArgumentException("Unexpected releaseNameAndInfo: " + Arrays.toString(releaseNameAndInfo));
        
        String releaseName = releaseNameAndInfo.length > 0 ? releaseNameAndInfo[0] : "API";
        String releaseVersion = releaseNameAndInfo.length > 1 ? releaseNameAndInfo[1] : "2.20.2";
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try ( Git git = Git.init().setDirectory(tmp.getRoot()).call() ) {
            git.add()
                .addFilepattern("downloads.tpl")
                .addFilepattern("releases.md")
                .call();
            
            git.commit()
                .setMessage("Initial commit")
                .call();
            
            Path releasesPath = Paths.get(new File(tmp.getRoot(), "releases.md").toURI());
            updater.updateReleases(releasesPath, releaseName, releaseVersion, releaseDate);
            
            List<DiffEntry> changes = git.diff().setOutputStream(out).call();
            
            // ensure that the diff we're getting only refers to the releases file
            // alternatively, when no changes are expected validate that
            
            if ( expectedLines.isEmpty() ) {
                assertThat("changes.size", changes.size(), equalTo(0));
                return;
            }

            assertThat("changes.size", changes.size(), equalTo(1));
            assertThat("changes[0].type", changes.get(0).getChangeType(), equalTo(ChangeType.MODIFY));
            assertThat("changes[0].path", changes.get(0).getPath(Side.NEW), equalTo("releases.md"));
            
            // now hack away on it safely
            List<String> ignoredPrefixes = Arrays.asList("diff", "index", "---", "+++", "@@");
            List<String> diffLines = Arrays.stream(new String(out.toByteArray(), StandardCharsets.UTF_8)
                .split("\\n"))
                .filter( l -> !ignoredPrefixes.stream().filter( p -> l.startsWith(p)).findAny().isPresent() )
                .collect(Collectors.toList());
            
            assertThat(diffLines, contains(expectedLines.toArray(new String[0])));
        }
    }

    @Test
    public void updateReleases_releaseAlreadyExists() throws IOException, GitAPIException {
        updateReleases0(LocalDateTime.of(2019, 2, 18, 22, 00), Collections.emptyList(), "Scripting JSP Tag Library", "2.4.0");
    }
    
    @Test
    public void updateReleases_releaseInNewMonth() throws IOException, GitAPIException {
        updateReleases0(LocalDateTime.of(2019, 3, 15, 22, 00), 
            Arrays.asList( 
               " ~~~~~~",
               " This is a list of all our releases, available from our [downloads](/downloads.cgi) page.",
               " ",
               "+## March 2019",
               "+",
               "+* API 2.20.2 (15th)",
               "+",
               " ## February 2019",
               " ",
               " * DataSource Provider 1.0.4, Resource Collection API 1.0.2, JCR ResourceResolver 3.0.18 (26th)"
            )
        );
    }

    @Test
    public void updateReleases_releaseExistingMonthAndDay() throws IOException, GitAPIException {
        updateReleases0(LocalDateTime.of(2019, 2, 26, 22, 00),
            Arrays.asList(
                " ", 
                " ## February 2019", 
                " ", 
                "-* DataSource Provider 1.0.4, Resource Collection API 1.0.2, JCR ResourceResolver 3.0.18 (26th)",
                "+* DataSource Provider 1.0.4, Resource Collection API 1.0.2, JCR ResourceResolver 3.0.18, API 2.20.2 (26th)", 
                " * Scripting JSP Tag Library 2.4.0, Scripting JSP Tag Library (Compat) 1.0.0 (18th)", 
                " * Pipes 3.1.0 (15th)", 
                " * Testing OSGi Mock 2.4.6 (14th)"
            )
        );
    }
}
