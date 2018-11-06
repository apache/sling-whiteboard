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
package org.apache.sling.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;

/**
 * Represents a bundle entry loaded from a Sling JAR. Contains the bundle
 * manifest data, start level, bundle contents and installation requirements.
 */
public class StartupBundleEntry extends BundleEntry {
    private static final Pattern ENTRY_PATTERN = Pattern.compile("resources\\/bundles\\/\\d{1,2}\\/[\\w\\-\\.]+\\.jar");

    public static boolean matches(JarEntry entry) {
        return ENTRY_PATTERN.matcher(entry.getName()).matches();
    }

    public StartupBundleEntry(JarEntry entry, InputStream is, BundleContext bundleContext) throws IOException {
        super(entry, is, bundleContext);
    }

}
