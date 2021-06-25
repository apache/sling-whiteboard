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

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.sitemap.SitemapInfo;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.common.SitemapUtil;
import org.apache.sling.sitemap.impl.SitemapServiceConfiguration;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

@Component(
        service = InventoryPrinter.class,
        property = {
                InventoryPrinter.NAME + "=slingsitemap",
                InventoryPrinter.TITLE + "=Sling Sitemap",
                InventoryPrinter.FORMAT + "=JSON",
                InventoryPrinter.FORMAT + "=TEXT",
                InventoryPrinter.WEBCONSOLE + "=true"

        }
)
public class SitemapInventoryPlugin implements InventoryPrinter {

    private static final Map<String, Object> AUTH = Collections.singletonMap(
            ResourceResolverFactory.SUBSERVICE, "sitemap-reader");
    private static final Logger LOG = LoggerFactory.getLogger(SitemapInventoryPlugin.class);

    @Reference
    private SitemapService sitemapService;
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private SitemapServiceConfiguration configuration;

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void print(PrintWriter printWriter, Format format, boolean isZip) {
        if (Format.JSON.equals(format)) {
            printJson(printWriter);
        } else if (Format.TEXT.equals(format)) {
            printText(printWriter);
        }
    }

    private void printJson(PrintWriter pw) {
        pw.print('{');
        pw.print("\"schedulers\":[");
        boolean hasScheduler = false;
        for (ServiceReference<?> ref : bundleContext.getBundle().getRegisteredServices()) {
            Object schedulerExp = ref.getProperty(Scheduler.PROPERTY_SCHEDULER_EXPRESSION);
            Object schedulerName = ref.getProperty(Scheduler.PROPERTY_SCHEDULER_NAME);
            if (schedulerExp instanceof String && schedulerName instanceof String) {
                if (hasScheduler) {
                    pw.print(',');
                }
                hasScheduler = true;
                pw.print("{\"name\":\"");
                pw.print(escapeDoubleQuotes((String) schedulerName));
                pw.print("\",\"expression\":\"");
                pw.print(escapeDoubleQuotes((String) schedulerExp));
                pw.print("\"}");
            }
        }
        pw.print("],");

        pw.print("\"roots\":{");
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            Iterator<Resource> roots = SitemapUtil.findSitemapRoots(resolver, "/");
            while (roots.hasNext()) {
                Resource root = roots.next();
                pw.print('"');
                pw.print(escapeDoubleQuotes(root.getPath()));
                pw.print("\":[");
                Iterator<SitemapInfo> infoIt = sitemapService.getSitemapInfo(root).iterator();
                while (infoIt.hasNext()) {
                    SitemapInfo info = infoIt.next();
                    pw.print('{');
                    pw.print("\"name\":\"");
                    pw.print(escapeDoubleQuotes(info.getName()));
                    pw.print('"');
                    pw.print(",\"url\":\"");
                    pw.print(escapeDoubleQuotes(info.getUrl()));
                    pw.print("\",\"status\":\"");
                    pw.print(info.getStatus());
                    pw.print('"');
                    if (info.getStoragePath() != null) {
                        pw.print(",\"path\":\"");
                        pw.print(escapeDoubleQuotes(info.getStoragePath()));
                        pw.print("\",\"size\":");
                        pw.print(info.getSize());
                        pw.print(",\"urls\":");
                        pw.print(info.getEntries());
                        pw.print(",\"inLimits\":");
                        pw.print(isWithinLimits(info));
                    }
                    pw.print('}');
                    if (infoIt.hasNext()) {
                        pw.print(',');
                    }
                }
                pw.print(']');
                if (roots.hasNext()) {
                    pw.print(',');
                }
            }
        } catch (LoginException ex) {
            pw.println("Failed to list sitemaps: " + ex.getMessage());
            LOG.warn("Failed to get inventory of sitemaps: {}", ex.getMessage(), ex);
        }
        pw.print('}');
        pw.print('}');
    }

    private void printText(PrintWriter pw) {
        pw.println("# Apache Sling Sitemap Schedulers");
        pw.println("# -------------------------------");
        pw.println("schedulers:");

        for (ServiceReference<?> ref : bundleContext.getBundle().getRegisteredServices()) {
            Object schedulerExp = ref.getProperty(Scheduler.PROPERTY_SCHEDULER_EXPRESSION);
            Object schedulerName = ref.getProperty(Scheduler.PROPERTY_SCHEDULER_NAME);
            if (schedulerExp != null && schedulerName != null) {
                pw.print(" - Name: ");
                pw.print(schedulerName);
                pw.println();
                pw.print("   Expression: ");
                pw.print(schedulerExp);
                pw.println();
            }
        }

        pw.println();
        pw.println();
        pw.println("# Apache Sling Sitemap Roots");
        pw.println("# --------------------------");
        pw.println("roots:");

        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            Iterator<Resource> roots = SitemapUtil.findSitemapRoots(resolver, "/");
            while (roots.hasNext()) {
                Resource root = roots.next();
                pw.print("  ");
                pw.print(root.getPath());
                pw.print(':');
                pw.println();
                for (SitemapInfo info : sitemapService.getSitemapInfo(root)) {
                    pw.print("   - Name: ");
                    pw.print(info.getName());
                    pw.println();
                    pw.print("     Url: ");
                    pw.print(info.getUrl());
                    pw.println();
                    pw.print("     Status: ");
                    pw.print(info.getStatus());
                    pw.println();
                    if (info.getStoragePath() != null) {
                        pw.print("     Path: ");
                        pw.print(info.getStoragePath());
                        pw.println();
                        pw.print("     Size: ");
                        pw.print(info.getSize());
                        pw.println();
                        pw.print("     Urls: ");
                        pw.print(info.getEntries());
                        pw.println();
                        pw.print("     Within Limits: ");
                        pw.print(isWithinLimits(info) ? "yes" : "no");
                        pw.println();
                    }
                }
            }
        } catch (LoginException ex) {
            pw.println("Failed to list sitemaps: " + ex.getMessage());
            LOG.warn("Failed to get inventory of sitemaps: {}", ex.getMessage(), ex);
        }
    }

    private boolean isWithinLimits(SitemapInfo info) {
        return info.getSize() <= configuration.getMaxSize() && info.getEntries() <= configuration.getMaxEntries();
    }

    private static String escapeDoubleQuotes(@Nullable String text) {
        return text == null ? "" : text.replace("\"", "\\\"");
    }
}
