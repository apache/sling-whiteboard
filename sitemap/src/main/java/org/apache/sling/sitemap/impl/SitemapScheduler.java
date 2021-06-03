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
import org.apache.sling.sitemap.generator.SitemapGenerator;
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

import static org.apache.sling.sitemap.impl.SitemapUtil.findSitemapRoots;

@Component(
        service = Runnable.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                Scheduler.PROPERTY_SCHEDULER_CONCURRENT + "=false",
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

        @AttributeDefinition(name = "Names", description = "A list of names for which this schedule should be used.")
        String[] names() default {SitemapGenerator.DEFAULT_SITEMAP};

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

    private String[] names;
    private String searchPath;

    @Activate
    protected void activate(Configuration configuration) {
        names = configuration.names();
        searchPath = configuration.searchPath();
    }

    @Override
    public void run() {
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            run(resolver);
        } catch (LoginException ex) {
            LOG.warn("Failed start sitemap jobs: {}", ex.getMessage(), ex);
        }
    }

    public void run(ResourceResolver resolver) {
        Iterator<Resource> sitemapRoots = findSitemapRoots(resolver, searchPath);
        while (sitemapRoots.hasNext()) {
            Resource sitemapRoot = sitemapRoots.next();
            Set<String> applicableNames = generatorManager.getApplicableNames(sitemapRoot, Arrays.asList(names));
            for (String applicableName : applicableNames) {
                Map<String, Object> jobProperties = new HashMap<>();
                jobProperties.put(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_NAME, applicableName);
                jobProperties.put(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_ROOT, sitemapRoot.getPath());
                Job job = jobManager.addJob(SitemapGeneratorExecutor.JOB_TOPIC, jobProperties);
                LOG.debug("Added job {}", job.getId());
            }
        }
    }
}
