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
package org.apache.sling.metrics.osgi.consumers.impl.dropwizard;

import org.apache.sling.metrics.osgi.StartupMetrics;
import org.apache.sling.metrics.osgi.StartupMetricsListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

@Component
public class SlingMetricsListener implements StartupMetricsListener{

    private static final String APPLICATION_STARTUP_GAUGE_NAME = "osgi.application_startup_time_millis";
    private static final String BUNDLE_STARTUP_GAUGE_NAME_PREFIX = "osgi.bundle_startup_time_millis.";
    private static final String SERVICE_RESTART_GAUGE_NAME_PREFIX = "osgi.service_restarts_count.";
    
    private static final long SERVICE_RESTART_THRESOLD = 3;
    private static final long SLOW_BUNDLE_THRESHOLD_MILLIS = 50;
    
    @Reference
    private MetricRegistry registry;
    
    @Override
    public void onStartupComplete(StartupMetrics event) {
        registry.register(APPLICATION_STARTUP_GAUGE_NAME, (Gauge<Long>) () -> event.getStartupTime().toMillis() );
        event.getBundleStartDurations().stream()
            .filter( bsd -> bsd.getStartedAfter().toMillis() >= SLOW_BUNDLE_THRESHOLD_MILLIS )
            .forEach( bsd -> registry.register(BUNDLE_STARTUP_GAUGE_NAME_PREFIX + bsd.getSymbolicName(), (Gauge<Long>) () -> bsd.getStartedAfter().toMillis()));
        event.getServiceRestarts().stream()
            .filter( src -> src.getServiceRestarts() >= SERVICE_RESTART_THRESOLD )
            .forEach( src -> registry.register(SERVICE_RESTART_GAUGE_NAME_PREFIX + src.getServiceIdentifier(), (Gauge<Integer>) src::getServiceRestarts) );
    }
}
