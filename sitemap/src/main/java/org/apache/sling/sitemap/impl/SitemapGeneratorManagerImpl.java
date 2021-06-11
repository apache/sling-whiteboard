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
import org.apache.sling.sitemap.generator.SitemapGeneratorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Component(service = SitemapGeneratorManager.class)
public class SitemapGeneratorManagerImpl implements SitemapGeneratorManager {

    private static final Logger LOG = LoggerFactory.getLogger(SitemapGeneratorManagerImpl.class);

    @Reference(service = SitemapGenerator.class, cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policyOption = ReferencePolicyOption.GREEDY)
    private List<SitemapGenerator> generators;
    @Reference
    private SitemapServiceConfiguration sitemapServiceConfiguration;

    @Activate
    protected void activate() {
        // highest ranked first
        Collections.reverse(generators);
    }

    @Override
    @Nullable
    public SitemapGenerator getGenerator(@NotNull Resource sitemapRoot, @NotNull String name) {
        for (SitemapGenerator generator : this.generators) {
            Set<String> providedNames = generator.getNames(sitemapRoot);

            if (providedNames.contains(name)) {
                return generator;
            }
        }

        return null;
    }

    @Override
    public Set<String> getNames(@NotNull Resource sitemapRoot) {
        return Collections.unmodifiableSet(getGenerators(sitemapRoot).keySet());
    }

    @Override
    @NotNull
    public Set<String> getNames(@NotNull Resource sitemapRoot, @NotNull Collection<String> retainOnly) {
        Set<String> allNames = new HashSet<>(getGenerators(sitemapRoot).keySet());
        allNames.retainAll(retainOnly);
        return Collections.unmodifiableSet(allNames);
    }

    @Override
    @NotNull
    public Map<String, SitemapGenerator> getGenerators(@NotNull Resource sitemapRoot) {
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

        return Collections.unmodifiableMap(consolidatedGenerators);
    }

    @Override
    @NotNull
    public Map<String, SitemapGenerator> getOnDemandGenerators(@Nullable Resource sitemapRoot) {
        Set<String> generators = sitemapServiceConfiguration.getOnDemandGenerators();
        return getGenerators(sitemapRoot).entrySet().stream()
                .filter(entry -> generators.contains(entry.getValue().getClass().getName()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    @NotNull
    public Set<String> getOnDemandNames(@NotNull Resource sitemapRoot) {
        return Collections.unmodifiableSet(getOnDemandGenerators(sitemapRoot).keySet());
    }

    @Override
    @NotNull
    public Set<String> getOnDemandNames(@NotNull Resource sitemapRoot, @NotNull Collection<String> retainOnly) {
        Set<String> allNames = new HashSet<>(getOnDemandGenerators(sitemapRoot).keySet());
        allNames.retainAll(retainOnly);
        return Collections.unmodifiableSet(allNames);
    }
}
