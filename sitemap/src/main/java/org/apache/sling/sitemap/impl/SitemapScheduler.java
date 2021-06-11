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

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.sitemap.generator.SitemapGeneratorManager;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.sling.sitemap.common.SitemapUtil.findSitemapRoots;

@Component(
        service = {SitemapScheduler.class, Runnable.class},
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                Scheduler.PROPERTY_SCHEDULER_CONCURRENT + ":Boolean=false",
                Scheduler.PROPERTY_SCHEDULER_RUN_ON + "=" + Scheduler.VALUE_RUN_ON_SINGLE
        }
)
@Designate(ocd = SitemapScheduler.Configuration.class, factory = true)
public class SitemapScheduler implements Runnable {

    @ObjectClassDefinition(name = "Apache Sling Sitemap - Scheduler")
    @interface Configuration {

        @AttributeDefinition(name = "Name", description = "The name of the scheduler configuration")
        String scheduler_name();

        @AttributeDefinition(name = "Schedule", description = "A cron expression defining the schedule at which the " +
                "sitemap generation jobs will be scheduled.")
        String scheduler_expression();

        @AttributeDefinition(name = "Generators", description = "A list of full qualified class names of " +
                "SitemapGenerator implementations. If set only the listed SitemapGenerators will be called. If left " +
                "empty all will be called.")
        String[] generators() default {};

        @AttributeDefinition(name = "Search Path", description = "The path under which sitemap roots should be " +
                "searched for")
        String searchPath() default "/content";
    }

    private static final Logger LOG = LoggerFactory.getLogger(SitemapScheduler.class);
    private static final Map<String, Object> AUTH = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE,
            "sitemap-reader");

    @Reference
    private JobManager jobManager;
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private SitemapGeneratorManager generatorManager;
    @Reference(target = "(subServiceName=sitemap-reader)")
    private ServiceUserMapped serviceUserMapped;

    private Set<String> generators;
    private String searchPath;

    @Activate
    protected void activate(Configuration configuration) {
        String[] configuredGenerators = configuration.generators();
        if (configuredGenerators != null && configuredGenerators.length > 0) {
            generators = Arrays.stream(configuredGenerators)
                    .filter(configuredGenerator -> !"".equals(configuredGenerator.trim()))
                    .collect(Collectors.toSet());
            if (generators.isEmpty()) {
                generators = null;
            }
        } else {
            generators = null;
        }
        searchPath = configuration.searchPath();
    }

    @Override
    public void run() {
        schedule(null);
    }

    public void schedule(@Nullable Collection<String> names) {
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            Iterator<Resource> sitemapRoots = findSitemapRoots(resolver, searchPath);
            while (sitemapRoots.hasNext()) {
                schedule(sitemapRoots.next(), names);
            }
        } catch (LoginException ex) {
            LOG.warn("Failed start sitemap jobs: {}", ex.getMessage(), ex);
        }
    }

    public void schedule(Resource sitemapRoot, @Nullable Collection<String> names) {
        Set<String> configuredNames = getNames(sitemapRoot);

        if (names != null) {
            configuredNames = new HashSet<>(configuredNames);
            configuredNames.retainAll(names);
        }

        for (String applicableName : configuredNames) {
            addJob(sitemapRoot.getPath(), applicableName);
        }
    }

    protected void addJob(String sitemapRoot, String applicableName) {
        Map<String, Object> jobProperties = new HashMap<>();
        jobProperties.put(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_NAME, applicableName);
        jobProperties.put(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_ROOT, sitemapRoot);
        Job job = jobManager.addJob(SitemapGeneratorExecutor.JOB_TOPIC, jobProperties);
        LOG.debug("Added job {}", job.getId());
    }

    /**
     * Returns the names for the given sitemap root. This depends on the configured generators. If no generators were
     * configured the names of all are returned. If some where configured the names provided only by those where the
     * class name matches are returned.
     *
     * @param sitemapRoot
     * @return
     */
    private Set<String> getNames(Resource sitemapRoot) {
        if (generators == null || generators.isEmpty()) {
            // all names
            return generatorManager.getGenerators(sitemapRoot).keySet();
        } else {
            // only those of the contained geneators
            return generatorManager.getGenerators(sitemapRoot).entrySet().stream()
                    .filter(entry -> generators.contains(entry.getValue().getClass().getName()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }
    }
}
