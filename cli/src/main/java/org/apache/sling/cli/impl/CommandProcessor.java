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
package org.apache.sling.cli.impl;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = CommandProcessor.class)
public class CommandProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private BundleContext ctx;

    private Map<CommandKey, CommandWithProps> commands = new ConcurrentHashMap<>();

    protected void activate(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Reference(service = Command.class, cardinality = MULTIPLE, policy = DYNAMIC)
    protected void bindCommand(Command cmd, Map<String, ?> props) {
        commands.put(CommandKey.of(props), CommandWithProps.of(cmd, props));
    }

    protected void unbindCommand(Map<String, ?> props) {
        commands.remove(CommandKey.of(props));
    }

    public void runCommand() {
        // TODO - remove duplication from CLI parsing code
        CommandKey key = CommandKey.of(ctx.getProperty("exec.args"));
        String target = parseTarget(ctx.getProperty("exec.args"));
        commands.getOrDefault(key, new CommandWithProps(ignored -> {
            logger.info("Usage: sling command sub-command [target]");
            logger.info("");
            logger.info("Available commands:");
            commands.forEach((k, c) -> logger.info("{} {}: {}", k.command, k.subCommand, c.summary));
        }, "")).cmd.execute(target);
        try {
            ctx.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(Framework.class).stop();
        } catch (BundleException e) {
            logger.warn("Failed running command", e);
        }
    }

    private String parseTarget(String cliSpec) {
        if (cliSpec == null || cliSpec.isEmpty())
            return null;

        String[] args = cliSpec.split(" ");
        if (args.length < 3)
            return null;
        
        return args[2];
    }
    

    static class CommandKey {

        private static final CommandKey EMPTY = new CommandKey("", "");

        private final String command;
        private final String subCommand;

        static CommandKey of(String cliSpec) {
            if (cliSpec == null || cliSpec.isEmpty())
                return EMPTY;

            String[] args = cliSpec.split(" ");
            if (args.length < 2)
                return EMPTY;

            return new CommandKey(args[0], args[1]);
        }

        static CommandKey of(Map<String, ?> serviceProps) {
            return new CommandKey((String) serviceProps.get(Command.PROPERTY_NAME_COMMAND), (String) serviceProps.get(Command.PROPERTY_NAME_SUBCOMMAND));
        }

        CommandKey(String command, String subCommand) {
            this.command = command;
            this.subCommand = subCommand;
        }

        @Override
        public int hashCode() {
            return Objects.hash(command, subCommand);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CommandKey other = (CommandKey) obj;
            return Objects.equals(command, other.command) && Objects.equals(subCommand, other.subCommand);
        }
    }
    
    static class CommandWithProps {
        private final Command cmd;
        private final String summary;

        static CommandWithProps of(Command cmd, Map<String, ?> props) {
            return new CommandWithProps(cmd, (String) props.get(Command.PROPERTY_NAME_SUMMARY));
        }
        
        CommandWithProps(Command cmd, String summary) {
            this.cmd = cmd;
            this.summary = summary;
        }
    }
}
