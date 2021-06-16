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

import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapGeneratorManagerImplTest {

    public final SlingContext context = new SlingContext();

    private final SitemapGeneratorManagerImpl subject = new SitemapGeneratorManagerImpl();
    private final SitemapServiceConfiguration sitemapServiceConfiguration = new SitemapServiceConfiguration();

    @Mock
    private SitemapGenerator generator1;
    @Mock
    private SitemapGenerator generator2;
    @Mock
    private SitemapGenerator generator3;

    @BeforeEach
    public void setup() {
        context.registerService(SitemapGenerator.class, generator1, "service.ranking", 1);
        context.registerService(SitemapGenerator.class, generator2, "service.ranking", 2);
        context.registerService(SitemapGenerator.class, generator3, "service.ranking", 3);

        context.registerInjectActivateService(new SitemapServiceConfiguration());
        context.registerInjectActivateService(subject);
    }

    @Test
    public void testAllGeneratorsReturnedWhenAllGenerateDifferentNames() {
        // given
        when(generator1.getNames(any())).thenReturn(Collections.singleton("sitemap1"));
        when(generator2.getNames(any())).thenReturn(Collections.singleton("sitemap2"));
        when(generator3.getNames(any())).thenReturn(Collections.singleton("sitemap3"));

        // when
        Map<String, SitemapGenerator> generators = subject.getGenerators(context.currentResource("/"));
        SitemapGenerator sitemap1Generator = subject.getGenerator(context.currentResource(), "sitemap1");
        SitemapGenerator sitemap2Generator = subject.getGenerator(context.currentResource(), "sitemap2");
        SitemapGenerator sitemap3Generator = subject.getGenerator(context.currentResource(), "sitemap3");


        // then
        assertThat(generators, aMapWithSize(3));
        assertThat(generators, hasEntry("sitemap1", generator1));
        assertThat(generators, hasEntry("sitemap2", generator2));
        assertThat(generators, hasEntry("sitemap3", generator3));
        assertEquals(generator1, sitemap1Generator);
        assertEquals(generator2, sitemap2Generator);
        assertEquals(generator3, sitemap3Generator);
    }

    @Test
    public void testAllGeneratorsOnDemand() {
        // given
        when(generator1.getNames(any())).thenReturn(Collections.singleton("sitemap1"));
        when(generator2.getNames(any())).thenReturn(Collections.singleton("sitemap2"));
        when(generator3.getNames(any())).thenReturn(Collections.singleton("sitemap3"));
        when(generator3.getOnDemandNames(any())).thenReturn(Collections.singleton("sitemap3"));

        // when
        Set<String> names = subject.getNames(context.currentResource("/"));
        Set<String> onDemandNames = subject.getOnDemandNames(context.currentResource("/"));

        // then
        assertThat(names, hasItems("sitemap1", "sitemap2", "sitemap3"));
        assertThat(onDemandNames, hasItems("sitemap3"));

        // and when
        MockOsgi.activate(subject, context.bundleContext(), "allOnDemand", Boolean.TRUE);
        names = subject.getNames(context.currentResource("/"));
        onDemandNames = subject.getOnDemandNames(context.currentResource("/"));

        // then
        assertThat(names, hasItems("sitemap1", "sitemap2", "sitemap3"));
        assertThat(onDemandNames, hasItems("sitemap1", "sitemap2", "sitemap3"));
    }

    @Test
    public void testThatHigherRankedGeneratorsTakePrecedenceOnConflictingNames() {
        // given
        when(generator1.getNames(any())).thenReturn(Collections.singleton(SitemapService.DEFAULT_SITEMAP_NAME));
        when(generator2.getNames(any())).thenReturn(Collections.singleton(SitemapService.DEFAULT_SITEMAP_NAME));
        when(generator3.getNames(any())).thenReturn(Collections.singleton("sitemap3"));

        // when
        Map<String, SitemapGenerator> generators = subject.getGenerators(context.currentResource("/"));
        SitemapGenerator sitemap3Generator = subject.getGenerator(context.currentResource(), "sitemap3");
        SitemapGenerator defaultSitemapGenerator = subject.getGenerator(context.currentResource(), SitemapService.DEFAULT_SITEMAP_NAME);
        Set<String> applicableNames = new HashSet<>(subject.getNames(context.currentResource()));
        applicableNames.retainAll(Arrays.asList(
                "sitemap1", "sitemap2", "sitemap3", SitemapService.DEFAULT_SITEMAP_NAME
        ));

        // then
        assertThat(generators, aMapWithSize(2));
        assertThat(generators, hasEntry(SitemapService.DEFAULT_SITEMAP_NAME, generator2));
        assertThat(generators, hasEntry("sitemap3", generator3));
        assertEquals(defaultSitemapGenerator, generator2);
        assertEquals(sitemap3Generator, generator3);
        assertThat(applicableNames, hasSize(2));
        assertThat(applicableNames, hasItem("sitemap3"));
        assertThat(applicableNames, hasItem(SitemapService.DEFAULT_SITEMAP_NAME));
    }
}
