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

import java.io.IOException;

import org.apache.sling.cli.impl.Command;
import org.apache.sling.cli.impl.jira.Version;
import org.apache.sling.cli.impl.jira.VersionFinder;
import org.apache.sling.cli.impl.nexus.StagingRepository;
import org.apache.sling.cli.impl.nexus.StagingRepositoryFinder;
import org.apache.sling.cli.impl.people.MembersFinder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Command.class, property = {
    Command.PROPERTY_NAME_COMMAND + "=release",
    Command.PROPERTY_NAME_SUBCOMMAND + "=prepare-email",
    Command.PROPERTY_NAME_SUMMARY + "=Prepares an email vote for the specified release." })
public class PrepareVoteEmailCommand implements Command {

    @Reference
    private MembersFinder membersFinder;

    // TODO - replace with file template
    private static final String EMAIL_TEMPLATE ="To: \"Sling Developers List\" <dev@sling.apache.org>\n" + 
            "Subject: [VOTE] Release ##RELEASE_NAME##\n" + 
            "\n" + 
            "Hi,\n" + 
            "\n" + 
            "We solved ##FIXED_ISSUES_COUNT## issues in this release:\n" + 
            "https://issues.apache.org/jira/browse/SLING/fixforversion/##VERSION_ID##\n" + 
            "\n" + 
            "Staging repository:\n" + 
            "https://repository.apache.org/content/repositories/orgapachesling-##RELEASE_ID##/\n" + 
            "\n" + 
            "You can use this UNIX script to download the release and verify the signatures:\n" + 
            "https://gitbox.apache.org/repos/asf?p=sling-tooling-release.git;a=blob;f=check_staged_release.sh;hb=HEAD\n" + 
            "\n" + 
            "Usage:\n" + 
            "sh check_staged_release.sh ##RELEASE_ID## /tmp/sling-staging\n" + 
            "\n" + 
            "Please vote to approve this release:\n" + 
            "\n" + 
            "  [ ] +1 Approve the release\n" + 
            "  [ ]  0 Don't care\n" + 
            "  [ ] -1 Don't release, because ...\n" + 
            "\n" + 
            "This majority vote is open for at least 72 hours.\n" +
            "\n" +
            "Regards,\n" +
            "##USER_NAME##\n" +
            "\n";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Reference
    private StagingRepositoryFinder repoFinder;
    
    @Reference
    private VersionFinder versionFinder;

    @Override
    public void execute(String target) {
        try {
            int repoId = Integer.parseInt(target);
            StagingRepository repo = repoFinder.find(repoId);
            Release release = Release.fromString(repo.getDescription());
            Version version = versionFinder.find(release.getName());
            
            String emailContents = EMAIL_TEMPLATE
                    .replace("##RELEASE_NAME##", release.getFullName())
                    .replace("##RELEASE_ID##", String.valueOf(repoId))
                    .replace("##VERSION_ID##", String.valueOf(version.getId()))
                    .replace("##FIXED_ISSUES_COUNT##", String.valueOf(version.getIssuesFixedCount()))
                    .replace("##USER_NAME##", membersFinder.getCurrentMember().getName());
                    
            logger.info(emailContents);

        } catch (IOException e) {
            logger.warn("Failed executing command", e);
        }
    }
}
