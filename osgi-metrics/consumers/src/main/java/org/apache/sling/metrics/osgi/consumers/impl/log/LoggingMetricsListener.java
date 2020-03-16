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
package org.apache.sling.metrics.osgi.consumers.impl.log;

import org.apache.sling.metrics.osgi.StartupMetrics;
import org.apache.sling.metrics.osgi.StartupMetricsListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class LoggingMetricsListener implements StartupMetricsListener {
    
    @ObjectClassDefinition(name = "Apache Sling Logging Startup Metrics Listener")
    public @interface Config {
        @AttributeDefinition(name = "Service Restart Threshold", description="Minimum number of service restarts during startup needed log the number of service restarts")
        int service_restart_threshold() default 3;
        @AttributeDefinition(name = "Slow Bundle Startup Threshold", description="Minimum bundle startup duration in milliseconds needed to log the bundle startup time")
        long slow_bundle_threshold_millis() default 200;
    }

    private int serviceRestartThreshold;
    private long slowBundleThresholdMillis;
    
    protected void activate(Config cfg) {
        this.serviceRestartThreshold = cfg.service_restart_threshold();
        this.slowBundleThresholdMillis = cfg.slow_bundle_threshold_millis();
    }
    
    @Override
    public void onStartupComplete(StartupMetrics event) {
        Logger log = LoggerFactory.getLogger(getClass());
        log.info("Application startup completed in {}", event.getStartupTime());
        event.getBundleStartDurations().stream()
            .filter( bsd -> bsd.getStartedAfter().toMillis() >= slowBundleThresholdMillis )
            .forEach( bsd -> log.info("Bundle {} started in {}", bsd.getSymbolicName(), bsd.getStartedAfter()) );
        
        event.getServiceRestarts().stream()
            .filter( src -> src.getServiceRestarts() >= serviceRestartThreshold )
            .forEach( src -> log.info("Service identified with {} was restarted {} times", src.getServiceIdentifier(), src.getServiceRestarts()));
    }

}
