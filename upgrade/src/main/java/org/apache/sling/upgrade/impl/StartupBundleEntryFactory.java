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

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;

import org.apache.sling.upgrade.EntryHandlerFactory;
import org.apache.sling.upgrade.StartupBundleEntry;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(service = { EntryHandlerFactory.class }, immediate = true)
public class StartupBundleEntryFactory implements EntryHandlerFactory<StartupBundleEntry> {

    private static final Pattern ENTRY_PATTERN = Pattern.compile("resources\\/bundles\\/\\d{1,2}\\/[\\w\\-\\.]+\\.jar");

    private BundleContext bundleContext;

    @Override
    public boolean matches(JarEntry entry) {
        return ENTRY_PATTERN.matcher(entry.getName()).matches();
    }

    @Activate
    public void activate(ComponentContext context) {
        this.bundleContext = context.getBundleContext();
    }

    @Override
    public StartupBundleEntry loadEntry(JarEntry entry, InputStream is) throws IOException {
        return new StartupBundleEntry(entry, is, bundleContext);
    }

}
