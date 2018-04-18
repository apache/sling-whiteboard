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
package org.apache.sling.feature.scanner.impl;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.scanner.ApplicationDescriptor;
import org.apache.sling.feature.scanner.BundleDescriptor;

/**
 * Information about an application.
 * This is the aggregated information.
 */
public class ApplicationDescriptorImpl
    extends ApplicationDescriptor {

    private BundleDescriptor frameworkDescriptor;

    private final Application app;

    public ApplicationDescriptorImpl(final Application app) {
        this.app = app;
    }

    @Override
    public Application getApplication() {
        return this.app;
    }

    @Override
    public BundleDescriptor getFrameworkDescriptor() {
        return frameworkDescriptor;
    }

    public void setFrameworkDescriptor(final BundleDescriptor frameworkDescriptor) {
        checkLocked();
        this.frameworkDescriptor = frameworkDescriptor;
    }
}