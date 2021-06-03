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
package org.apache.sling.sitemap.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(service = SitemapGeneratorManager.class)
public class SitemapGeneratorManager {

    private static final Logger LOG = LoggerFactory.getLogger(SitemapGeneratorManager.class);

    @Reference(service = SitemapGenerator.class, cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policyOption = ReferencePolicyOption.GREEDY)
    private List<SitemapGenerator> generators;

    @Activate
    protected void activate() {
        // highest ranked first
        Collections.reverse(generators);
    }

    @Nullable
    public SitemapGenerator getGenerator(Resource sitemapRoot, String name) {
        for (SitemapGenerator generator : this.generators) {
            Set<String> providedNames = generator.getNames(sitemapRoot);

            if (providedNames.contains(name)) {
                return generator;
            }
        }

        return null;
    }

    @NotNull
    public Set<String> getApplicableNames(Resource sitemapRoot, Collection<String> candidates) {
        Set<String> result = new HashSet<>(candidates.size());
        for (SitemapGenerator generator : this.generators) {
            for (String providedName : generator.getNames(sitemapRoot)) {
                if (candidates.contains(providedName)) {
                    result.add(providedName);
                }
                if (candidates.size() == result.size()) {
                    // early exit
                    return result;
                }
            }
        }
        return result;
    }

    @NotNull
    public Map<String, SitemapGenerator> getGenerators(@Nullable Resource sitemapRoot) {
        Map<String, SitemapGenerator> consolidatedGenerators = new HashMap<>();

        for (SitemapGenerator generator : this.generators) {
            Set<String> providedNames = generator.getNames(sitemapRoot);
            Set<String> names = new HashSet<>(providedNames);

            if (names.removeAll(consolidatedGenerators.keySet()) && LOG.isDebugEnabled()) {
                String alreadyGenerated = String.join(",", consolidatedGenerators.keySet());
                String provided = String.join(",", providedNames);
                LOG.debug("Removed duplicated names. Already generated: '{}', provided by {}: '{}'",
                        alreadyGenerated, generator.getClass().getName(), provided);
            }

            if (names.isEmpty()) {
                LOG.debug("Skipping {} as it did not provide any names for '{}'", generator.getClass().getName(),
                        sitemapRoot.getPath());
                continue;
            }

            for (String name : names) {
                consolidatedGenerators.put(name, generator);
            }
        }

        return consolidatedGenerators;
    }
}
