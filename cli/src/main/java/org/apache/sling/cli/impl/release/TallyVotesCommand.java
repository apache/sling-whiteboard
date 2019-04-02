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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.sling.cli.impl.Command;
import org.apache.sling.cli.impl.mail.Email;
import org.apache.sling.cli.impl.mail.EmailThread;
import org.apache.sling.cli.impl.mail.VoteThreadFinder;
import org.apache.sling.cli.impl.nexus.StagingRepository;
import org.apache.sling.cli.impl.nexus.StagingRepositoryFinder;
import org.apache.sling.cli.impl.people.Member;
import org.apache.sling.cli.impl.people.MembersFinder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Command.class, property = {
    Command.PROPERTY_NAME_COMMAND+"=release",
    Command.PROPERTY_NAME_SUBCOMMAND+"=tally-votes",
    Command.PROPERTY_NAME_SUMMARY+"=Counts votes cast for a release and generates the result email"
})
public class TallyVotesCommand implements Command {

    @Reference
    private MembersFinder membersFinder;

    // TODO - move to file
    private static final String EMAIL_TEMPLATE =
            "To: \"Sling Developers List\" <dev@sling.apache.org>\n" + 
            "Subject: [RESULT] [VOTE] Release ##RELEASE_NAME##\n" + 
            "\n" + 
            "Hi,\n" + 
            "\n" + 
            "The vote has passed with the following result:\n" +
            "\n" + 
            "+1 (binding): ##BINDING_VOTERS##\n" + 
            "+1 (non-binding): ##NON_BINDING_VOTERS##\n" +
            "\n" +
            "I will copy this release to the Sling dist directory and\n" + 
            "promote the artifacts to the central Maven repository.\n" +
            "\n" +
            "Regards,\n" +
            "##USER_NAME##\n" +
            "\n";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private StagingRepositoryFinder repoFinder;
    
    @Reference
    private VoteThreadFinder voteThreadFinder;
    
    @Override
    public void execute(String target) {
        try {
            
            StagingRepository repository = repoFinder.find(Integer.parseInt(target));
            Release release = Release.fromString(repository.getDescription());
            EmailThread voteThread = voteThreadFinder.findVoteThread(release.getFullName());

            Set<String> bindingVoters = new HashSet<>();
            Set<String> nonBindingVoters = new HashSet<>();
            for (Email e : voteThread.getEmails()) {
                if (isPositiveVote(e)) {
                    String sender = e.getFrom().replaceAll("<.*>", "").trim();
                    for (Member m : membersFinder.findMembers()) {
                        if (sender.equals(m.getName())) {
                            if (m.isPMCMember()) {
                                bindingVoters.add(sender);
                            } else {
                                nonBindingVoters.add(sender);
                            }
                        }
                    }
                }
            }
            String email = EMAIL_TEMPLATE
                .replace("##RELEASE_NAME##", release.getFullName())
                .replace("##BINDING_VOTERS##", String.join(", ", bindingVoters))
                .replace("##USER_NAME##", membersFinder.getCurrentMember().getName());
            if (nonBindingVoters.isEmpty()) {
                email = email.replace("##NON_BINDING_VOTERS##", "none");
            } else {
                email = email.replace("##NON_BINDING_VOTERS##", String.join(", ", nonBindingVoters));
            }

            logger.info(email);
            
        } catch (IOException e) {
            logger.warn("Command execution failed", e);
        }
    }

    // TODO - better detection of '+1' votes
    private boolean isPositiveVote(Email e) {
        return cleanup(e.getBody()).contains("+1");
    }

    private String cleanup(String subject) {
        String[] lines = subject.split("\\n");
        return Arrays.stream(lines)
            .filter( l -> !l.isEmpty() )
            .filter( l -> !l.startsWith(">"))
            .collect(Collectors.joining("\n"));
    }

}
