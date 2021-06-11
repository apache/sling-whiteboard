/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.remotecontent.samples.graphql;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import javax.jcr.Session;

import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SlingDataFetcher.class, property = {"name=samples/command"})
public class CommandDataFetcher implements SlingDataFetcher<Object> {

    public static final String ARG_INPUT = "input";
    public static final String ARG_LANG = "lang";

    @Reference
    private RepoInitParser parser;

    @Reference
    private JcrRepoInitOpsProcessor processor;

    // TODO turn these into services and provide a GraphQL query to list them
    abstract static  class CommandLanguage {
        public final String name;
        public final String help;

        protected CommandLanguage(String name, String help) {
            this.name = name;
            this.help = help;
        }

        abstract CommandResult execute(@NotNull SlingDataFetcherEnvironment e);
    }
    private static final Map<String, CommandLanguage> languages = new HashMap<>();

    private void addLanguage(CommandLanguage language) {
        languages.put(language.name, language);
    }

    public CommandDataFetcher() {
        addLanguage(new CommandLanguage(
            "repoinit", 
            "See https://sling.apache.org/documentation/bundles/repository-initialization.html for information about the repoinit language") {
                public CommandResult execute(@NotNull SlingDataFetcherEnvironment e) {
                    final Map<String, Object> result = new HashMap<>();
                    final String script = e.getArgument(ARG_INPUT, "");
                    String output;
                    boolean success;
                    try {
                        final Session s = e.getCurrentResource().getResourceResolver().adaptTo(Session.class);
                        processor.apply(s, parser.parse(new StringReader(script)));
                        s.save();
                        success = true;
                        output = "repoinit script successfully executed";
                    } catch(Exception ex) {
                        success = false;
                        output = ex.toString();
                    }
                    return new CommandResult(success, output, help);
            
                }
            });
            addLanguage(new CommandLanguage(
                "echo", 
                "Echoes its input") {
                    public CommandResult execute(@NotNull SlingDataFetcherEnvironment e) {
                        final Object input = e.getArgument(ARG_INPUT, "");
                        return new CommandResult(input != null, input, help);
                    }
                });
    }

    @Override
    public @Nullable Object get(@NotNull SlingDataFetcherEnvironment e) throws Exception {
        final String lang = e.getArgument(ARG_LANG, null);
        final CommandLanguage language = languages.get(lang);
        if(language == null) {
            throw new RuntimeException(String.format("Unsupported language '%s' (provided by %s argument)", lang, ARG_LANG));
        }
        return language.execute(e);
    }
    
}
