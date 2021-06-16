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

import com.google.common.collect.ImmutableSet;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapServiceImplSchedulingTest {

    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private final SitemapServiceImpl subject = new SitemapServiceImpl();
    private final SitemapStorage storage = new SitemapStorage();
    private final SitemapGeneratorManagerImpl generatorManager = new SitemapGeneratorManagerImpl();
    private final SitemapServiceConfiguration sitemapServiceConfiguration = new SitemapServiceConfiguration();

    @Mock
    private ServiceUserMapped serviceUser;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JobManager jobManager;
    private SitemapGenerator generator1 = new SitemapGenerator() {
        @Override
        public @NotNull Set<String> getNames(@NotNull Resource sitemapRoot) {
            return Collections.singleton(SitemapService.DEFAULT_SITEMAP_NAME);
        }

        @Override
        public void generate(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Sitemap sitemap, @NotNull GenerationContext context) throws SitemapException {
            fail();
        }
    };
    private SitemapGenerator generator2 = new SitemapGenerator() {
        @Override
        public @NotNull Set<String> getNames(@NotNull Resource sitemapRoot) {
            return ImmutableSet.of("foo");
        }

        @Override
        public void generate(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Sitemap sitemap, @NotNull GenerationContext context) throws SitemapException {
            fail();
        }
    };

    private Resource siteRoot;
    private Resource micrositeRoot;
    private SitemapScheduler schedulerWithGenerator1OnSite;
    private SitemapScheduler schedulerWithGenerator2OnMicrosite;

    @BeforeEach
    public void setup() throws LoginException {
        siteRoot = context.create().resource("/content/site/de", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        micrositeRoot = context.create().resource("/content/microsite/de", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));

        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-writer");
        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-reader");
        context.registerService(SitemapGenerator.class, generator1);
        context.registerService(SitemapGenerator.class, generator2);
        context.registerService(JobManager.class, jobManager);
        context.registerInjectActivateService(sitemapServiceConfiguration);
        context.registerInjectActivateService(generatorManager);
        context.registerInjectActivateService(storage);
        context.registerInjectActivateService(subject);

        schedulerWithGenerator1OnSite = context.registerInjectActivateService(spy(new SitemapScheduler()),
                "searchPath", "/content/site"
        );
        schedulerWithGenerator2OnMicrosite = context.registerInjectActivateService(spy(new SitemapScheduler()),
                "searchPath", "/content/microsite"
        );

        SitemapSchedulerTest.initResourceResolver(context, schedulerWithGenerator1OnSite, this::setupResourceResolver);
        SitemapSchedulerTest.initResourceResolver(context, schedulerWithGenerator2OnMicrosite, this::setupResourceResolver);
    }

    private void setupResourceResolver(ResourceResolver resolver) {
        MockJcr.setQueryResult(
                resolver.adaptTo(Session.class),
                "/jcr:root/content/site//*[@sling:sitemapRoot=true]" +
                        " option(index tag slingSitemaps)",
                Query.XPATH,
                Collections.singletonList(siteRoot.adaptTo(Node.class))
        );
        MockJcr.setQueryResult(
                resolver.adaptTo(Session.class),
                "/jcr:root/content/microsite//*[@sling:sitemapRoot=true]" +
                        " option(index tag slingSitemaps)",
                Query.XPATH,
                Collections.singletonList(micrositeRoot.adaptTo(Node.class))
        );
    }

    @Test
    public void testAllSchedulersCalled() {
        // when
        subject.scheduleGeneration();

        // then
        verify(schedulerWithGenerator1OnSite, atLeastOnce()).addJob(any(), any());
        verify(schedulerWithGenerator2OnMicrosite, atLeastOnce()).addJob(any(), any());
    }

    @Test
    public void testSchedulersCalledForName() {
        // when
        subject.scheduleGeneration("<default>");

        // then
        verify(schedulerWithGenerator1OnSite, atLeastOnce()).addJob(any(), eq("<default>"));
        verify(schedulerWithGenerator2OnMicrosite, atLeastOnce()).addJob(any(), eq("<default>"));
    }

    @Test
    public void testSchedulersCalledForPath() {
        // when
        subject.scheduleGeneration(siteRoot);

        // then
        verify(schedulerWithGenerator1OnSite, atLeastOnce()).addJob(eq(siteRoot.getPath()), any());
        verify(schedulerWithGenerator2OnMicrosite, never()).addJob(any(), any());
    }

    @Test
    public void testSchedulersCalledForPathAndName() {
        // when
        subject.scheduleGeneration(siteRoot, "foo");
        subject.scheduleGeneration(micrositeRoot, "foo");

        // then
        verify(schedulerWithGenerator1OnSite, times(1)).addJob(eq(siteRoot.getPath()), eq("foo"));
        verify(schedulerWithGenerator2OnMicrosite, times(1)).addJob(eq(micrositeRoot.getPath()), eq("foo"));
    }
}
