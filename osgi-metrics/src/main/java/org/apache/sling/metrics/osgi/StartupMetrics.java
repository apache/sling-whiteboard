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
package org.apache.sling.metrics.osgi;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// TODO - create interface, to hide away constructor
public final class StartupMetrics {

    private final Instant jvmStartup;
    private final Duration startupTime;
    private final List<BundleStartDuration> bundleStartDurations;
    private final List<ServiceRestartCounter> serviceRestarts;
 
    public StartupMetrics(Instant jvmStartup, Duration startupTime, List<BundleStartDuration> bundleStartDurations, List<ServiceRestartCounter> serviceRestarts) {
        this.jvmStartup = jvmStartup;
        this.startupTime = startupTime;
        this.bundleStartDurations = bundleStartDurations;
        this.serviceRestarts = serviceRestarts;
    }

    public Instant getJvmStartup() {
        return jvmStartup;
    }
    
    public Duration getStartupTime() {
        return startupTime;
    }
    
    public List<BundleStartDuration> getBundleStartDurations() {
        return bundleStartDurations;
    }
    
    public List<ServiceRestartCounter> getServiceRestarts() {
        return serviceRestarts;
    }
}
