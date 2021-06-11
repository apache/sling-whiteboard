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

import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    public void testThatHigherRankedGeneratorsTakePrecedenceOnConflictingNames() {
        // given
        when(generator1.getNames(any())).thenReturn(Collections.singleton(SitemapGenerator.DEFAULT_SITEMAP));
        when(generator2.getNames(any())).thenReturn(Collections.singleton(SitemapGenerator.DEFAULT_SITEMAP));
        when(generator3.getNames(any())).thenReturn(Collections.singleton("sitemap3"));

        // when
        Map<String, SitemapGenerator> generators = subject.getGenerators(context.currentResource("/"));
        SitemapGenerator sitemap3Generator = subject.getGenerator(context.currentResource(), "sitemap3");
        SitemapGenerator defaultSitemapGenerator = subject.getGenerator(context.currentResource(), SitemapGenerator.DEFAULT_SITEMAP);
        Set<String> applicableNames = subject.getNames(context.currentResource(), Arrays.asList(
                "sitemap1", "sitemap2", "sitemap3", SitemapGenerator.DEFAULT_SITEMAP
        ));

        // then
        assertThat(generators, aMapWithSize(2));
        assertThat(generators, hasEntry(SitemapGenerator.DEFAULT_SITEMAP, generator2));
        assertThat(generators, hasEntry("sitemap3", generator3));
        assertEquals(defaultSitemapGenerator, generator2);
        assertEquals(sitemap3Generator, generator3);
        assertThat(applicableNames, hasSize(2));
        assertThat(applicableNames, hasItem("sitemap3"));
        assertThat(applicableNames, hasItem(SitemapGenerator.DEFAULT_SITEMAP));
    }
}
