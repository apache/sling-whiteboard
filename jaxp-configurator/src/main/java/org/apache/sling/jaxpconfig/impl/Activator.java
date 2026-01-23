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
package org.apache.sling.jaxpconfig.impl;

import java.util.Map;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Ensures that secure processing system properties are set when the bundle is started.
 */
@Header(name = "Bundle-Activator", value = "${@class}")
public class Activator implements BundleActivator {

    private static final Map<String, String> PROPERTIES = Map.of(
            "javax.xml.accessExternalDTD", "",
            "javax.xml.accessExternalSchema", "",
            "javax.xml.accessExternalStylesheet", "");

    @Override
    public void start(final BundleContext context) {
        // not logging to make sure that no dependencies are requires, as this bundle should
        // start as early as possible, before any XML processing takes place
        PROPERTIES.forEach(System::setProperty);
    }

    @Override
    public void stop(final BundleContext context) {
        // intentionally not unsetting the property
    }
}
