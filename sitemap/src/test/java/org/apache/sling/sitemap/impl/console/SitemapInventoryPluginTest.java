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
package org.apache.sling.sitemap.impl.console;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.felix.inventory.Format;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.sitemap.SitemapInfo;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.impl.SitemapServiceConfiguration;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapInventoryPluginTest {

    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);
    private final SitemapInventoryPlugin subject = new SitemapInventoryPlugin();
    private final SitemapServiceConfiguration configuration = new SitemapServiceConfiguration();

    // some terms are different in text then in json for better readability
    private static Map<String, String> YAML_TO_JSON = ImmutableMap.of(
            "within limits", "inlimits"
    );

    @Mock
    private SitemapService sitemapService;
    @Mock
    private SitemapInfo deInfo;
    @Mock
    private SitemapInfo enInfo1;
    @Mock
    private SitemapInfo enInfo2;
    @Mock
    private ServiceReference<Runnable> schedulerReference1;
    @Mock
    private ServiceReference<Runnable> schedulerReference2;
    @Mock
    private ServiceReference<Runnable> unknownReference;
    // we have to use a mock bundle as the osgi-mock one does not implement Bundle#getRegisteredServices()
    @Mock
    private Bundle bundle;
    @Mock
    private BundleContext bundleContext;
    // we have to use mock rrf in order to provide the context#resourceResolver() rr to the implementation with the
    // mocked jcr query responses
    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    @BeforeEach
    public void setup() throws LoginException {
        ResourceResolver resourceResolver = spy(context.resourceResolver());

        context.registerService(SitemapService.class, sitemapService);
        context.registerInjectActivateService(configuration, "maxEntries", 999);
        context.registerService(ResourceResolverFactory.class, resourceResolverFactory, "service.ranking", 100);
        context.registerInjectActivateService(subject);
        subject.activate(bundleContext);

        Resource deRoot = context.create().resource("/content/site/de",
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE);
        Resource enRoot = context.create().resource("/content/site/en",
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE);

        MockJcr.setQueryResult(
                context.resourceResolver().adaptTo(Session.class),
                "/jcr:root//*[@sling:sitemapRoot=true] option(index tag slingSitemaps)",
                Query.XPATH,
                Arrays.asList(deRoot.adaptTo(Node.class), enRoot.adaptTo(Node.class))
        );

        when(deInfo.getName()).thenReturn(SitemapService.DEFAULT_SITEMAP_NAME);
        when(deInfo.getUrl()).thenReturn("/site/de.sitemap.xml");
        when(deInfo.getStatus()).thenReturn(SitemapInfo.Status.STORAGE);
        when(deInfo.getSize()).thenReturn(1000);
        when(deInfo.getEntries()).thenReturn(10);
        when(deInfo.getStoragePath()).thenReturn("/var/sitemaps/content/site/de/sitemap.xml");
        when(enInfo1.getName()).thenReturn(SitemapService.SITEMAP_INDEX_NAME);
        when(enInfo1.getUrl()).thenReturn("/site/en.sitemap-index.xml");
        when(enInfo1.getStatus()).thenReturn(SitemapInfo.Status.ON_DEMAND);
        when(enInfo2.getName()).thenReturn(SitemapService.DEFAULT_SITEMAP_NAME);
        when(enInfo2.getUrl()).thenReturn("/site/en.sitemap.xml");
        when(enInfo2.getStatus()).thenReturn(SitemapInfo.Status.STORAGE);
        when(enInfo2.getSize()).thenReturn(10000);
        when(enInfo2.getEntries()).thenReturn(1000);
        when(enInfo2.getStoragePath()).thenReturn("/var/sitemaps/content/site/en/sitemap.xml");

        when(bundleContext.getBundle()).thenReturn(bundle);
        when(bundle.getRegisteredServices()).thenReturn(new ServiceReference[]{
                schedulerReference1, schedulerReference2, unknownReference
        });
        when(schedulerReference1.getProperty(Scheduler.PROPERTY_SCHEDULER_NAME)).thenReturn("sitemap-default");
        when(schedulerReference1.getProperty(Scheduler.PROPERTY_SCHEDULER_EXPRESSION)).thenReturn("0 0 0 * * * ?");
        when(schedulerReference2.getProperty(Scheduler.PROPERTY_SCHEDULER_NAME)).thenReturn("sitemap-news");
        when(schedulerReference2.getProperty(Scheduler.PROPERTY_SCHEDULER_EXPRESSION)).thenReturn("0 */30 * * * * ?");
        when(resourceResolverFactory.getServiceResourceResolver(any())).thenReturn(resourceResolver);

        doNothing().when(resourceResolver).close();
        doReturn(Collections.singleton(deInfo))
                .when(sitemapService).getSitemapInfo(argThat(resourceWithPath(deRoot.getPath())));
        doReturn(Arrays.asList(enInfo1, enInfo2))
                .when(sitemapService).getSitemapInfo(argThat(resourceWithPath(enRoot.getPath())));
    }

    @Test
    public void testJson() {
        // given
        StringWriter writer = new StringWriter();

        // when
        subject.print(new PrintWriter(writer), Format.JSON, false);

        // then
        assertJson("SitemapInventoryPluginTest/inventory.json", writer.toString());
    }

    @Test
    public void testText() {
        // given
        StringWriter writer = new StringWriter();

        // when
        subject.print(new PrintWriter(writer), Format.TEXT, false);

        // then
        assertYaml("SitemapInventoryPluginTest/inventory.json", writer.toString());
    }

    private static void assertYaml(String expected, String given) {
        assertJson(new YAMLMapper(), expected, given);
    }

    private static void assertJson(String expected, String given) {
        assertJson(new ObjectMapper(), expected, given);
    }

    private static void assertJson(ObjectMapper objectMapper, String expected, String given) {
        try {
            InputStream expectedResource = SitemapInventoryPluginTest.class.getClassLoader()
                    .getResourceAsStream(expected);
            StringWriter expectedContent = new StringWriter();
            IOUtils.copy(expectedResource, expectedContent, StandardCharsets.UTF_8);
            JsonNode expectedJson = objectMapper.readTree(normalizeJson(expectedContent.toString()));
            JsonNode givenJson = objectMapper.readTree(normalizeJson(given));
            assertEquals(expectedJson, givenJson);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    private static String normalizeJson(String json) {
        String lowercase = json.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : YAML_TO_JSON.entrySet()) {
            lowercase = lowercase.replace(entry.getKey(), entry.getValue());
        }
        return lowercase;
    }

    private static ArgumentMatcher<Resource> resourceWithPath(String path) {
        return r -> r.getPath().equals(path);
    }
}
