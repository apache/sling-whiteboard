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
package org.apache.sling.launchpad.startupmanager;

import org.apache.sling.launchpad.api.StartupListener;
import org.apache.sling.launchpad.api.StartupMode;
import org.apache.sling.launchpad.api.StartupService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

public class StartupListenerTracker implements FrameworkListener, BundleListener {

    private static final Logger log = LoggerFactory.getLogger(StartupListenerTracker.class);

    private static final int TARGET_START_LEVEL = 30;

    private final StartupMode startupMode;

    private final BundleContext bundleContext;

    private final ServiceTracker<StartupListener, StartupListener> tracker;

    private final ServiceReference<StartLevel> startLevelServiceReference;

    private final StartLevel startLevelService;

    private volatile boolean frameworkStarted;

    private boolean startLevelBased;

    StartupListenerTracker(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.startupMode = StartupMode.INSTALL;

        startLevelServiceReference = bundleContext.getServiceReference(StartLevel.class);
        if (startLevelServiceReference != null) {
            startLevelService = bundleContext.getService(startLevelServiceReference);
        } else {
            startLevelService = null;
        }

        tracker = new ServiceTracker<>(bundleContext, StartupListener.class,
                new ServiceTrackerCustomizer<StartupListener, StartupListener>() {
                    @Override
                    public void removedService(final ServiceReference<StartupListener> reference, final StartupListener service) {
                        bundleContext.ungetService(reference);
                    }

                    @Override
                    public void modifiedService(final ServiceReference<StartupListener> reference, final StartupListener service) {
                        // nothing to do
                    }

                    @Override
                    public StartupListener addingService(final ServiceReference<StartupListener> reference) {
                        final StartupListener listener = bundleContext.getService(reference);
                        if (listener != null) {
                            try {
                                listener.inform(startupMode, frameworkStarted);
                            } catch (final Throwable t) {
                                log.error("Error calling StartupListener {}", listener, t);
                            }
                        }
                        return listener;
                    }
                });
        tracker.open();
        bundleContext.addFrameworkListener(this);
        bundleContext.addBundleListener(this);
    }

    public void close() {
        bundleContext.removeFrameworkListener(this);
        bundleContext.removeBundleListener(this);
        tracker.close();
        if (startLevelServiceReference != null) {
            bundleContext.ungetService(startLevelServiceReference);
        }
    }

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED && startLevelService != null) {
            startLevelBased = true;
            int startLevel = startLevelService.getStartLevel();
            if (startLevel >= TARGET_START_LEVEL) {
                onFinished();
            }
        } else if (event.getType() == FrameworkEvent.STARTED && !startLevelBased) {
            onFinished();
        }
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED && !frameworkStarted) {
            Bundle[] allBundles = bundleContext.getBundles();
            int total = allBundles.length;
            int started = 0;
            for (Bundle b : allBundles) {
                if (b.getState() == Bundle.ACTIVE) {
                    started++;
                }
            }
            float ratio = 0;
            if (total > 0) {
                ratio = (float) started / total;
            }
            log.info("Startup progress: {}% (bundles {}/{})", new Object[] {(int) (ratio * 100), started, total});
            for (StartupListener listener : tracker.getServices(new StartupListener[0])) {
                listener.startupProgress(ratio);
            }
        }
    }

    private void onFinished() {
        frameworkStarted = true;
        log.info("Startup finished");
        for (StartupListener listener : tracker.getServices(new StartupListener[0])) {
            listener.startupFinished(startupMode);
        }

        StartupService startupService = new StartupService() {
            @Override
            public StartupMode getStartupMode() {
                return startupMode;
            }
        };
        bundleContext.registerService(StartupService.class, startupService, new Hashtable<String, Object>());
    }


}
