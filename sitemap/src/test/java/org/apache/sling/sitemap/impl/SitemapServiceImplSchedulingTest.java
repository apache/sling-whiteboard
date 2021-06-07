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
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapServiceImplSchedulingTest {

    public final SlingContext context = new SlingContext();

    private final SitemapServiceImpl subject = new SitemapServiceImpl();
    private final SitemapStorage storage = new SitemapStorage();
    private final SitemapGeneratorManager generatorManager = new SitemapGeneratorManager();

    @Mock
    private ServiceUserMapped serviceUser;
    @Mock
    private JobManager jobManager;
    @Mock
    private SitemapGenerator generator;

    private Resource root1;
    private Resource root2;
    private SitemapScheduler scheduler1;
    private SitemapScheduler scheduler2;

    @BeforeEach
    public void setup() {
        root1 = context.create().resource("/content/site/de", Collections.singletonMap(
                "sitemapRoot", Boolean.TRUE
        ));
        root2 = context.create().resource("/content/microsite/de", Collections.singletonMap(
                "sitemapRoot", Boolean.TRUE
        ));

        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-writer");
        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-reader");
        context.registerService(SitemapGenerator.class, generator);
        context.registerService(JobManager.class, jobManager);
        context.registerInjectActivateService(generatorManager);
        context.registerInjectActivateService(storage);
        context.registerInjectActivateService(subject);

        scheduler1 = context.registerInjectActivateService(spy(new SitemapScheduler()),
                "names", "<default>",
                "searchPath", "/content/site"
        );
        scheduler2 = context.registerInjectActivateService(spy(new SitemapScheduler()),
                "names", new String[]{"<default>", "foo"},
                "searchPath", "/content/microsite"
        );
    }

    @Test
    public void testAllSchedulersCalled() {
        // when
        subject.scheduleGeneration();

        // then
        verify(scheduler1, times(1)).run();
        verify(scheduler2, times(1)).run();
    }

    @Test
    public void testAllSchedulersCalledForName() {
        // when
        subject.scheduleGeneration("<default>");

        // then
        verify(scheduler1, times(1)).run("<default>");
        verify(scheduler2, times(1)).run("<default>");
    }

    @Test
    public void testSchedulersCalledForName() {
        // when
        subject.scheduleGeneration("foo");

        // then
        verify(scheduler1, never()).run();
        verify(scheduler2, times(1)).run("foo");
    }

    @Test
    public void testSchedulersCalledForPath() {
        // when
        subject.scheduleGeneration(root1);

        // then
        verify(scheduler1, times(1)).run(root1);
        verify(scheduler2, never()).run();
    }

    @Test
    public void testSchedulersCalledForPathAndName() {
        // when
        subject.scheduleGeneration(root1, "foo");
        subject.scheduleGeneration(root2, "foo");

        // then
        verify(scheduler1, never()).run(eq(root1), argThat(collection -> collection.contains("foo")));
        verify(scheduler2, times(1)).run(eq(root2), argThat(collection -> collection.contains("foo")));
    }
}
