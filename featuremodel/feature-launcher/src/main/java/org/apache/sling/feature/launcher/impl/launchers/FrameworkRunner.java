/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.launcher.impl.launchers;

import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Launcher directly using the OSGi launcher API.
 */
public class FrameworkRunner extends AbstractRunner {

    public FrameworkRunner(final Map<String, String> frameworkProperties,
            final Map<Integer, List<File>> bundlesMap,
            final List<Object[]> configurations,
            final List<File> installables) throws Exception {
        super(configurations, installables);

        final ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class);
        FrameworkFactory factory = null;
        for(FrameworkFactory f : loader) {
            factory = f;
            break;
        }
        if ( factory == null ) {
            throw new Exception("Unable to locate framework factory.");
        }

        // create the framework
        final Framework framework = factory.newFramework(frameworkProperties);
        // initialize the framework
        framework.init();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    framework.stop();
                } catch (final BundleException e) {
                    // ignore
                }
            }
        });

        this.setupFramework(framework, bundlesMap);

        // finally start
        framework.start();
    }
}
