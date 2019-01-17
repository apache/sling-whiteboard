/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.upgrade.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.info.InfoProvider;
import org.apache.sling.installer.api.info.Resource;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.upgrade.BundleEntry;
import org.apache.sling.upgrade.EntryHandlerFactory;
import org.apache.sling.upgrade.UpgradeResultEntry;
import org.apache.sling.upgrade.UpgradeResultEntry.RESULT;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { EntryHandlerFactory.class }, immediate = true)
public class BundleEntryFactory implements EntryHandlerFactory<BundleEntry> {

    private static final Logger log = LoggerFactory.getLogger(BundleEntryFactory.class);

    @Reference
    private SlingSettingsService settingsService;

    @Reference
    private InfoProvider infoProvider;

    @Reference
    private OsgiInstaller installer;

    private static final Pattern ENTRY_PATTERN = Pattern
            .compile("resources\\/install(.[a-z_]+)?\\/\\d{1,2}\\/[\\w\\-\\.]+\\.jar");
    private BundleContext bundleContext;

    @Override
    public boolean matches(JarEntry entry) {
        boolean matches = ENTRY_PATTERN.matcher(entry.getName()).matches();
        if (matches && entry.getName().split("\\/")[1].contains(".")) {
            String runmode = StringUtils.substringAfter(entry.getName().split("\\/")[1], ".");
            return settingsService.getRunModes().contains(runmode);
        }
        return matches;
    }

    @Activate
    public void activate(ComponentContext context) {
        this.bundleContext = context.getBundleContext();
    }

    @Override
    public BundleEntry loadEntry(JarEntry entry, InputStream is) throws IOException {
        return new BundleEntry(entry, is, bundleContext);
    }

    @Override
    public UpgradeResultEntry<BundleEntry> processEntry(BundleEntry entry) {
        UpgradeResultEntry<BundleEntry> result = null;
        if (!entry.isInstalled() || entry.isUpdated()) {
            try {
                log.debug("Installing bundle {}", entry.getOriginalName());
                Bundle bundle = bundleContext.installBundle("launchpad:" + entry.getOriginalName(),
                        new ByteArrayInputStream(entry.getContents()));
                bundle.start();
                log.debug("Bundle installed correctly!");
                result = new UpgradeResultEntry<>(entry);

            } catch (Exception e) {
                log.error("Exception installing bundle", e);
                result = new UpgradeResultEntry<>(entry, e);
            }
        } else {
            result = new UpgradeResultEntry<>(entry, null, RESULT.NO_OPERATION);
        }
        return result;
    }

}
