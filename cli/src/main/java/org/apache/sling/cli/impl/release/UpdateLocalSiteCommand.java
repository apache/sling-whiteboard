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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.apache.sling.cli.impl.Command;
import org.apache.sling.cli.impl.jbake.JBakeContentUpdater;
import org.apache.sling.cli.impl.nexus.StagingRepository;
import org.apache.sling.cli.impl.nexus.StagingRepositoryFinder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Command.class, property = {
    Command.PROPERTY_NAME_COMMAND+"=release",
    Command.PROPERTY_NAME_SUBCOMMAND+"=update-local-site",
    Command.PROPERTY_NAME_SUMMARY+"=Updates the Sling website with the new release information, based on a local checkout"
})
public class UpdateLocalSiteCommand implements Command {
    
    private static final String GIT_CHECKOUT = "/tmp/sling-site";

    @Reference
    private StagingRepositoryFinder repoFinder;
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    public void execute(String target) {
        
        
        try {
            ensureRepo();
            try ( Git git = Git.open(new File(GIT_CHECKOUT)) ) {
                
                StagingRepository repository = repoFinder.find(Integer.parseInt(target));
                ReleaseVersion releaseVersion = ReleaseVersion.fromRepositoryDescription(repository.getDescription());
                
                JBakeContentUpdater updater = new JBakeContentUpdater();
        
                Path templatePath = Paths.get(GIT_CHECKOUT, "src", "main", "jbake", "templates", "downloads.tpl");
                Path releasesPath = Paths.get(GIT_CHECKOUT, "src", "main", "jbake", "content", "releases.md");
                updater.updateDownloads(templatePath, releaseVersion.getComponent(), releaseVersion.getVersion());
                updater.updateReleases(releasesPath, releaseVersion.getComponent(), releaseVersion.getVersion(), LocalDateTime.now());
        
                git.diff()
                    .setOutputStream(System.out)
                    .call();
            }
        } catch (GitAPIException | IOException e) {
            logger.warn("Failed executing command", e);
        }
            
    }

    private void ensureRepo() throws GitAPIException, IOException {
        
        if ( !Paths.get(GIT_CHECKOUT).toFile().exists() ) {
            Git.cloneRepository()
            .setURI("https://github.com/apache/sling-site.git")
            .setProgressMonitor(new TextProgressMonitor())
            .setDirectory(new File(GIT_CHECKOUT))
            .call();
        } else {
            try ( Git git = Git.open(new File(GIT_CHECKOUT)) )  {
                git.reset()
                    .setMode(ResetType.HARD)
                    .call();
            }
        }
    }
}
