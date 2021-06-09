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
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.builder.Sitemap;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Fields;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapSchedulerTest {

    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private final SitemapScheduler subject = new SitemapScheduler();
    private final SitemapGeneratorManager generatorManager = new SitemapGeneratorManager();

    private final TestGenerator generator1 = new TestGenerator() {};
    private final TestGenerator generator2 = new TestGenerator() {};

    @Mock
    private ServiceUserMapped serviceUser;
    @Mock
    private JobManager jobManager;

    private Resource rootDe;
    private Resource rootEn;
    private Resource rootEnContent;

    @BeforeEach
    public void setup() {
        rootDe = context.create().resource("/content/site/de", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE));
        rootEn = context.create().resource("/content/site/en");
        rootEnContent = context.create().resource("/content/site/en/jcr:content", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE));

        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-reader");
        context.registerService(JobManager.class, jobManager);
        context.registerService(SitemapGenerator.class, generator1, "service.ranking", 1);
        context.registerService(SitemapGenerator.class, generator2, "service.ranking", 2);
        context.registerInjectActivateService(generatorManager);

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
        context.registerInjectActivateService(subject);
        initResourceResolver(subject, resolver -> MockJcr.setQueryResult(
                resolver.adaptTo(Session.class),
                "/jcr:root/content//*[@" + SitemapService.PROPERTY_SITEMAP_ROOT + "=true]",
                Query.XPATH,
                ImmutableList.of(
                        rootDe.adaptTo(Node.class),
                        rootEnContent.adaptTo(Node.class)
                )
        ));
        generator1.setNames(SitemapGenerator.DEFAULT_SITEMAP);
        generator2.setNames(SitemapGenerator.DEFAULT_SITEMAP);

        // when
        subject.run();

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
        context.registerInjectActivateService(subject);
        initResourceResolver(subject, resolver -> MockJcr.setQueryResult(
                resolver.adaptTo(Session.class),
                "/jcr:root/content//*[@" + SitemapService.PROPERTY_SITEMAP_ROOT + "=true]",
                Query.XPATH,
                Collections.singletonList(rootDe.adaptTo(Node.class))
        ));
        generator1.setNames(SitemapGenerator.DEFAULT_SITEMAP, "sitemap1");
        generator2.setNames(SitemapGenerator.DEFAULT_SITEMAP, "sitemap2");

        // when
        subject.run();

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

    @Test
    public void testNothingScheduledWhenNameDoesNotMatchGeneratorFromConfiguration() {
        // given
        context.registerInjectActivateService(subject, "generators", new String[]{
                generator1.getClass().getName()
        });
        initResourceResolver(subject, resolver -> MockJcr.setQueryResult(
                resolver.adaptTo(Session.class),
                "/jcr:root/content//*[@" + SitemapService.PROPERTY_SITEMAP_ROOT + "=true]",
                Query.XPATH,
                Collections.singletonList(rootDe.adaptTo(Node.class))
        ));
        generator1.setNames("sitemap1");
        generator2.setNames(SitemapGenerator.DEFAULT_SITEMAP, "sitemap2");

        // when
        subject.schedule(Collections.singleton(SitemapGenerator.DEFAULT_SITEMAP));

        // then
        verify(jobManager, never()).addJob(any(), any());

        // and when
        subject.schedule(Collections.singleton("sitemap1"));

        // then
        verify(jobManager, times(1)).addJob(
                eq("org/apache/sling/sitemap/build"),
                argThat(sitemapJobPropertiesMatch("sitemap1", "/content/site/de"))
        );
    }

    private void initResourceResolver(SitemapScheduler scheduler, Consumer<ResourceResolver> resolverConsumer) {
        initResourceResolver(context, scheduler, resolverConsumer);
    }

    static void initResourceResolver(SlingContext context, SitemapScheduler scheduler,
                                     Consumer<ResourceResolver> resolverConsumer) {
        try {
            ResourceResolverFactory original = context.getService(ResourceResolverFactory.class);
            ResourceResolverFactory mock = mock(ResourceResolverFactory.class);
            Fields.allDeclaredFieldsOf(scheduler).instanceFields().stream()
                    .filter(instanceField -> "resourceResolverFactory".equals(instanceField.name()))
                    .forEach(instanceField -> instanceField.set(mock));

            lenient().when(mock.getServiceResourceResolver(any())).then(inv -> {
                ResourceResolver resolver = original.getServiceResourceResolver(inv.getArgument(0));
                resolverConsumer.accept(resolver);
                return resolver;
            });
        } catch (LoginException ex) {
            fail("Did not expect LoginException while mocking.");
        }
    }

    private static ArgumentMatcher<Map<String, Object>> sitemapJobPropertiesMatch(String name, String path) {
        return map -> map.get("sitemap.root").equals(path) && map.get("sitemap.name").equals(name);
    }

    private static class TestGenerator implements SitemapGenerator {

        private Set<String> names;

        TestGenerator(String... names) {
            this.names = new HashSet<>(Arrays.asList(names));
        }

        public void setNames(String... names) {
            this.names = new HashSet<>(Arrays.asList(names));
        }

        @Override
        public @NotNull Set<String> getNames(@NotNull Resource sitemapRoot) {
            return names;
        }

        @Override
        public void generate(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Sitemap sitemap, @NotNull GenerationContext context) throws SitemapException {
            fail();
        }
    }
}
