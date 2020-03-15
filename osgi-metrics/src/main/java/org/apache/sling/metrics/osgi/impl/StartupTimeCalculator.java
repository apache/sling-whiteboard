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
package org.apache.sling.metrics.osgi.impl;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.sling.metrics.osgi.BundleStartDuration;
import org.apache.sling.metrics.osgi.ServiceRestartCounter;
import org.apache.sling.metrics.osgi.StartupMetrics;
import org.apache.sling.metrics.osgi.StartupMetricsListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class StartupTimeCalculator {

    // delay activation until the system is marked as ready
    // don't explicitly import the systemready class as this bundle must be started as early as possible in order
    // to record bundle starup times
    
    private ServiceTracker<Object, Object> readyTracker;
    private ServiceTracker<StartupMetricsListener, StartupMetricsListener> listenersTracker;
    private BundleStartTimeCalculator bundleCalculator;
    private ServiceRestartCountCalculator serviceCalculator;
    private ScheduledExecutorService executor;
    private Future<Void> future;

    public StartupTimeCalculator(BundleContext ctx, BundleStartTimeCalculator bundleCalculator, ServiceRestartCountCalculator serviceCalculator) throws InvalidSyntaxException {
        executor = Executors.newScheduledThreadPool(1);
        this.bundleCalculator = bundleCalculator;
        this.serviceCalculator = serviceCalculator;
        this.readyTracker = new ServiceTracker<>(ctx, 
                ctx.createFilter("(" + Constants.OBJECTCLASS+"=org.apache.felix.systemready.SystemReady)"),
                new ServiceTrackerCustomizerAdapter<Object, Object>() {

                    @Override
                    public Object addingService(ServiceReference<Object> reference) {
                        if ( future == null ) 
                            future = calculate();
                        return ctx.getService(reference);
                    }
                    
                    @Override
                    public void removedService(ServiceReference<Object> reference, Object service) {
                        if ( future != null && !future.isDone() ) {
                            boolean cancelled = future.cancel(false);
                            if ( cancelled )
                                future = null;
                        }
                    }
                });
        this.readyTracker.open();
        
        this.listenersTracker = new ServiceTracker<>(ctx, StartupMetricsListener.class, new ServiceTrackerCustomizerAdapter<StartupMetricsListener, StartupMetricsListener>() {
            @Override
            public StartupMetricsListener addingService(ServiceReference<StartupMetricsListener> reference) {
                return ctx.getService(reference);
            }
        });
        this.listenersTracker.open();
    }
    
    public void close() {
        this.readyTracker.close();
    }

    private Future<Void> calculate() {

        long currentMillis = Clock.systemUTC().millis();
        
        return executor.schedule(() -> {
            long startupMillis = ManagementFactory.getRuntimeMXBean().getStartTime();
            
            Duration startupDuration = Duration.ofMillis(currentMillis - startupMillis);
            Instant startupInstant = Instant.ofEpochMilli(startupMillis);
            List<BundleStartDuration> bundleDurations = bundleCalculator.getBundleStartDurations();
            List<ServiceRestartCounter> serviceRestarts = serviceCalculator.getServiceRestartCounters();
            
            for ( StartupMetricsListener listener : listenersTracker.getServices(new StartupMetricsListener[0]) )
                listener.onStartupComplete(new StartupMetrics(startupInstant, startupDuration, bundleDurations, serviceRestarts));
            
            return null;
        }, 5, TimeUnit.SECONDS);
        

    }
}