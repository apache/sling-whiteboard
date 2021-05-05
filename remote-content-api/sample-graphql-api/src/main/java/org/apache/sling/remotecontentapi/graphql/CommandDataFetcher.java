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

package org.apache.sling.remotecontentapi.graphql;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

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

    @Reference
    private RepoInitParser parser;

    @Reference
    private JcrRepoInitOpsProcessor processor;

    private static final String LANG_REPOINIT = "repoinit";

    @Override
    public @Nullable Object get(@NotNull SlingDataFetcherEnvironment e) throws Exception {
        final String lang = e.getArgument("lang", LANG_REPOINIT);
        if(!LANG_REPOINIT.equals(lang)) {
            throw new RuntimeException("For now, the only supported language is " + LANG_REPOINIT);
        }
        final String script = e.getArgument("script");
        final Map<String, Object> result = new HashMap<>();

        try {
            final Session s = e.getCurrentResource().getResourceResolver().adaptTo(Session.class);
            processor.apply(s, parser.parse(new StringReader(script)));
            s.save();
            result.put("success", true);
            result.put("output", "repoinit script successfully executed");
        } catch(Exception ex) {
            result.put("success", false);
            result.put("output", ex.toString());
        }
        
        
        return result;
    }
    
}
