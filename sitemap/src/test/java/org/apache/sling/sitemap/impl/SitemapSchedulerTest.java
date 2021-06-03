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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapSchedulerTest {

    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private final SitemapScheduler subject = new SitemapScheduler();
    private final SitemapGeneratorManager generatorManager = new SitemapGeneratorManager();

    @Mock
    private ServiceUserMapped serviceUser;
    @Mock
    private SitemapGenerator generator1;
    @Mock
    private SitemapGenerator generator2;
    @Mock
    private JobManager jobManager;

    private Resource rootDe;
    private Resource rootEn;
    private Resource rootEnContent;

    @BeforeEach
    public void setup() {
        rootDe = context.create().resource("/content/site/de", Collections.singletonMap(
                "sitemapRoot", Boolean.TRUE));
        rootEn = context.create().resource("/content/site/en");
        rootEnContent = context.create().resource("/content/site/en/jcr:content", Collections.singletonMap(
                "sitemapRoot", Boolean.TRUE));

        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-reader");
        context.registerService(JobManager.class, jobManager);
        context.registerService(SitemapGenerator.class, generator1, "service.ranking", 1);
        context.registerService(SitemapGenerator.class, generator2, "service.ranking", 2);
        context.registerInjectActivateService(generatorManager);
        context.registerInjectActivateService(subject, "names", new String[]{
                SitemapGenerator.DEFAULT_SITEMAP, "sitemap1", "sitemap2"});

        AtomicInteger jobCount = new AtomicInteger(0);

        when(jobManager.addJob(any(), any())).then(inv -> {
            Job job = mock(Job.class);
            when(job.getId()).thenReturn(String.valueOf(jobCount.incrementAndGet()));
            return job;
        });
    }

    @Test
    public void testOneDefaultSitemapJobStartedForEachRoot() {
        // given
        MockJcr.setQueryResult(
                context.resourceResolver().adaptTo(Session.class),
                "/jcr:root/content//*[@sitemapRoot=true]",
                Query.XPATH,
                ImmutableList.of(
                        rootDe.adaptTo(Node.class),
                        rootEnContent.adaptTo(Node.class)
                )
        );
        when(generator1.getNames(any())).thenReturn(Collections.singleton(SitemapGenerator.DEFAULT_SITEMAP));
        when(generator2.getNames(any())).thenReturn(Collections.singleton(SitemapGenerator.DEFAULT_SITEMAP));

        // when
        subject.run(context.resourceResolver());

        // then
        verify(jobManager, times(1)).addJob(
                eq("org/apache/sling/sitemap/build"),
                argThat(sitemapJobPropertiesMatch(SitemapGenerator.DEFAULT_SITEMAP, "/content/site/de"))
        );
        verify(jobManager, times(1)).addJob(
                eq("org/apache/sling/sitemap/build"),
                argThat(sitemapJobPropertiesMatch(SitemapGenerator.DEFAULT_SITEMAP, "/content/site/en"))
        );
    }

    @Test
    public void testOneSitemapJobStartedForEachName() {
        // given
        MockJcr.setQueryResult(
                context.resourceResolver().adaptTo(Session.class),
                "/jcr:root/content//*[@sitemapRoot=true]",
                Query.XPATH,
                Collections.singletonList(rootDe.adaptTo(Node.class))
        );
        when(generator1.getNames(any())).thenReturn(ImmutableSet.of(SitemapGenerator.DEFAULT_SITEMAP, "sitemap1"));
        when(generator2.getNames(any())).thenReturn(ImmutableSet.of(SitemapGenerator.DEFAULT_SITEMAP, "sitemap2"));

        // when
        subject.run(context.resourceResolver());

        // then
        verify(jobManager, times(1)).addJob(
                eq("org/apache/sling/sitemap/build"),
                argThat(sitemapJobPropertiesMatch(SitemapGenerator.DEFAULT_SITEMAP, "/content/site/de"))
        );
        verify(jobManager, times(1)).addJob(
                eq("org/apache/sling/sitemap/build"),
                argThat(sitemapJobPropertiesMatch("sitemap1", "/content/site/de"))
        );
        verify(jobManager, times(1)).addJob(
                eq("org/apache/sling/sitemap/build"),
                argThat(sitemapJobPropertiesMatch("sitemap2", "/content/site/de"))
        );
    }

    private static ArgumentMatcher<Map<String, Object>> sitemapJobPropertiesMatch(String name, String path) {
        return map -> map.get("sitemap.root").equals(path) && map.get("sitemap.name").equals(name);
    }
}
