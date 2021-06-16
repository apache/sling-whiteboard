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
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

@Component(service = SitemapGeneratorManager.class)
@Designate(ocd = SitemapGeneratorManagerImpl.Configuration.class)
public class SitemapGeneratorManagerImpl implements SitemapGeneratorManager {

    @ObjectClassDefinition(name = "Apache Sling Sitemap - Sitemap Generator Manager")
    @interface Configuration {

        @AttributeDefinition(name = "All on-demand", description = "If enabled, forces all registered " +
                "SitemapGenerators to serve all sitemaps on-demand.")
        boolean allOnDemand() default false;
    }

    private static final Logger LOG = LoggerFactory.getLogger(SitemapGeneratorManagerImpl.class);

    @Reference(service = SitemapGenerator.class, cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policyOption = ReferencePolicyOption.GREEDY)
    private List<SitemapGenerator> generators;

    private boolean allOnDemand;

    @Activate
    protected void activate(Configuration configuration) {
        // highest ranked first
        Collections.reverse(generators);
        allOnDemand = configuration.allOnDemand();
    }

    @Override
    @Nullable
    public SitemapGenerator getGenerator(@NotNull Resource sitemapRoot, @NotNull String name) {
        for (SitemapGenerator generator : this.generators) {
            Set<String> providedNames = new HashSet<>(generator.getNames(sitemapRoot));

            if (providedNames.contains(name)) {
                return generator;
            }
        }

        return null;
    }

    @Override
    public Set<String> getNames(@NotNull Resource sitemapRoot) {
        return getGenerators(sitemapRoot).keySet();
    }

    @Override
    @NotNull
    public Map<String, SitemapGenerator> getGenerators(@NotNull Resource sitemapRoot) {
        return Collections.unmodifiableMap(consolidateGenerators(sitemapRoot, generator -> generator::getNames));
    }

    @Override
    @NotNull
    public Set<String> getOnDemandNames(@NotNull Resource sitemapRoot) {
        return allOnDemand ? getNames(sitemapRoot) : Collections.unmodifiableSet(
                consolidateGenerators(sitemapRoot, generator -> generator::getOnDemandNames).keySet());
    }

    private Map<String, SitemapGenerator> consolidateGenerators(Resource sitemapRoot,
            Function<SitemapGenerator, Function<Resource, Set<String>>> namesProvider) {
        Map<String, SitemapGenerator> consolidatedGenerators = new HashMap<>();

        for (SitemapGenerator generator : this.generators) {
            Set<String> providedNames = namesProvider.apply(generator).apply(sitemapRoot);
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
