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
package org.apache.sling.feature.analyser.task.impl;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

public class CheckBundlesForInitialContent implements AnalyserTask {

    /** The manifest header to specify initial content to be loaded. */
    private static final String CONTENT_HEADER = "Sling-Initial-Content";

    /**
     * The path directive specifying the target node where initial content will
     * be loaded.
     */
    private static final String PATH_DIRECTIVE = "path";

    @Override
    public String getName() {
        return "Bundle Initial Content Check";
    }

    @Override
    public String getId() {
        return "bundle-content";
    }

    @Override
    public void execute(final AnalyserTaskContext ctx) {
        // check for initial content
        for(final BundleDescriptor info : ctx.getDescriptor().getBundleDescriptors()) {
            final List<String> initialContent = extractInitialContent(info.getManifest());

            if ( !initialContent.isEmpty() ) {
                ctx.reportWarning("Found initial content in " + info.getArtifact() + " : " + initialContent);
            }
        }
    }

    private List<String> extractInitialContent(final Manifest m) {
        final List<String> initialContent = new ArrayList<>();
        if ( m != null ) {
            final String root =  m.getMainAttributes().getValue(CONTENT_HEADER);
            if (root != null) {
                Clause[] clauses = Parser.parseHeader(root);
                for (final Clause entry :clauses) {

                    String path = entry.getDirective(PATH_DIRECTIVE);
                    if (path == null) {
                        path = "/";
                    } else if (!path.startsWith("/")) {
                        // make relative path absolute
                        path = "/" + path;
                    }
                    initialContent.add(path);
                }
            }
        }
        return initialContent;
    }
}
