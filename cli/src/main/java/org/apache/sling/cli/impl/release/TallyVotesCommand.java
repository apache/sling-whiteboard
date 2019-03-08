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

import org.apache.sling.cli.impl.Command;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Command.class, property = {
    Command.PROPERTY_NAME_COMMAND+"=release",
    Command.PROPERTY_NAME_SUBCOMMAND+"=tally-votes",
    Command.PROPERTY_NAME_SUMMARY+"=Counts votes cast for a release and generates the result email"
})
public class TallyVotesCommand implements Command {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void execute(String target) {
        logger.info("Tallying votes for release {}", target);

    }

}
