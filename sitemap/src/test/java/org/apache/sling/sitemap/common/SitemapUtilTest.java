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
package org.apache.sling.sitemap.common;

import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SlingContextExtension.class})
public class SitemapUtilTest {

    public final SlingContext context = new SlingContext();

    @Test
    public void testSingleRootSitemapName() {
        // given
        Resource root = context.create().resource("/content/site/de", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));

        // when
        String unnamedSitemap = SitemapUtil.getSitemapSelector(root, root, SitemapService.DEFAULT_SITEMAP_NAME);
        String namedSitemap = SitemapUtil.getSitemapSelector(root, root, "foo");

        // then
        assertEquals("sitemap", unnamedSitemap);
        assertEquals("foo-sitemap", namedSitemap);
    }

    @Test
    public void testMultiRootSitemapName() {
        // given
        context.create().resource("/content/site/de", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        Resource secondLevelRoot = context.create().resource("/content/site/de/faqs", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        Resource thirdLevelRoot = context.create().resource("/content/site/de/faqs/many", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));

        // when
        Resource topLevelRoot = SitemapUtil.getTopLevelSitemapRoot(thirdLevelRoot);
        String unnamedSecondLevelSitemap = SitemapUtil.getSitemapSelector(secondLevelRoot, topLevelRoot, SitemapService.DEFAULT_SITEMAP_NAME);
        String namedSecondLevelSitemap = SitemapUtil.getSitemapSelector(secondLevelRoot, topLevelRoot, "foo");
        String unnamedThirdLevelSitemap = SitemapUtil.getSitemapSelector(thirdLevelRoot, topLevelRoot, SitemapService.DEFAULT_SITEMAP_NAME);
        String namedThirdLevelSitemap = SitemapUtil.getSitemapSelector(thirdLevelRoot, topLevelRoot, "bar");

        // then
        assertEquals("faqs-sitemap", unnamedSecondLevelSitemap);
        assertEquals("faqs-foo-sitemap", namedSecondLevelSitemap);
        assertEquals("faqs-many-sitemap", unnamedThirdLevelSitemap);
        assertEquals("faqs-many-bar-sitemap", namedThirdLevelSitemap);
    }

    @Test
    public void testSitemapResolutionFromFileName() {
        // given
        Resource root = context.create().resource("/content/site/de", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        Resource news = context.create().resource("/content/site/de/news", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        context.create().resource("/content/site/de/products");
        Resource productPage = context.create().resource("/content/site/de/products/product-page", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        Resource product = context.create().resource("/content/site/de/products/product", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        List<Function<String, String>> selectorMutations = Arrays.asList(
                Function.identity(),
                // mutate the selctor and append a file index
                selector -> selector + "-" + 2
        );

        // when
        for (Function<String, String> mutator : selectorMutations) {
            Map<Resource, String> invalid = SitemapUtil.resolveSitemapRoots(root, mutator.apply("foobar"));
            Map<Resource, String> defaultSitemap = SitemapUtil.resolveSitemapRoots(root, mutator.apply("sitemap"));
            Map<Resource, String> newsCandidates = SitemapUtil.resolveSitemapRoots(root, mutator.apply("news-sitemap"));
            Map<Resource, String> productPageCandidates = SitemapUtil.resolveSitemapRoots(root, mutator.apply("products-product-page-tops-sitemap"));
            Map<Resource, String> productCandidates = SitemapUtil.resolveSitemapRoots(root, mutator.apply("products-sitemap"));

            // then
            assertThat(invalid, aMapWithSize(0));
            assertThat(defaultSitemap, aMapWithSize(1));
            assertThat(defaultSitemap.entrySet(), hasInOrder(anEntry(root, SitemapService.DEFAULT_SITEMAP_NAME)));
            assertThat(newsCandidates, aMapWithSize(2));
            assertThat(newsCandidates.entrySet(), hasInOrder(
                    anEntry(root, "news"),
                    anEntry(news, SitemapService.DEFAULT_SITEMAP_NAME))
            );
            assertThat(productPageCandidates, aMapWithSize(3));
            assertThat(productPageCandidates.entrySet(), hasInOrder(
                    anEntry(root, "products-product-page-tops"),
                    anEntry(product, "page-tops"),
                    anEntry(productPage, "tops")
            ));
            assertThat(productCandidates, aMapWithSize(1));
            assertThat(productCandidates.entrySet(), hasInOrder(anEntry(root, "products")));
        }
    }


    private static <V> Matcher<Iterable<? extends Map.Entry<Resource, V>>> hasInOrder(Matcher<Map.Entry<Resource, V>>... entries) {
        return IsIterableContainingInOrder.contains(entries);
    }

    private static <V> Matcher<Map.Entry<Resource, V>> anEntry(Resource key, V value) {
        Matcher<Resource> keyMatcher = equalTo(key);
        Matcher<V> valueMatcher = Matchers.equalTo(value);
        return new CustomMatcher<Map.Entry<Resource, V>>("key = " + key.getPath() + ", value = " + value) {
            @Override
            public boolean matches(Object o) {
                return o instanceof Map.Entry
                        && keyMatcher.matches(((Map.Entry<?, ?>) o).getKey())
                        && valueMatcher.matches(((Map.Entry<?, ?>) o).getValue());
            }
        };
    }

    private static Matcher<Resource> equalTo(Resource resource) {
        return new CustomMatcher<Resource>("resource with path " + resource.getPath()) {
            @Override
            public boolean matches(Object o) {
                return o instanceof Resource && ((Resource) o).getPath().equals(resource.getPath());
            }
        };
    }
}
