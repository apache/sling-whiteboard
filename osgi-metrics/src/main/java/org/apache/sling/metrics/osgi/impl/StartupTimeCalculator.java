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
import java.util.Arrays;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO - consider not depending on logging and simply providing an API to get the information
// or maybe late binding to the logging API by using Dynamic-ImportPackage
public class StartupTimeCalculator {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // delay activation until the system is marked as ready
    // don't explicitly import the systemready class as this bundle must be started as early as possible in order
    // to record bundle starup times
    
    private List<Dumpable> dumpables;
    private ServiceTracker<Object, Object> readyTracker;

    
    public StartupTimeCalculator(BundleContext ctx, Dumpable... dumpables) throws InvalidSyntaxException {
        this.dumpables = Arrays.asList(dumpables);
        this.readyTracker = new ServiceTracker<>(ctx, 
                ctx.createFilter("(" + Constants.OBJECTCLASS+"=org.apache.felix.systemready.SystemReady)"),
                new ServiceTrackerCustomizer<Object, Object>() {

                    @Override
                    public Object addingService(ServiceReference<Object> reference) {
                        calculate();
                        return reference;
                    }

                    @Override
                    public void modifiedService(ServiceReference<Object> reference, Object service) {
                        // nothing to do here
                    }

                    @Override
                    public void removedService(ServiceReference<Object> reference, Object service) {
                        // nothing to do here
                    }
                });
        this.readyTracker.open();
    }
    
    public void close() {
        this.readyTracker.close();
    }

    private void calculate() {

        long currentMillis = Clock.systemUTC().millis();
        long startupMillis = ManagementFactory.getRuntimeMXBean().getStartTime();

        logger.info("Application startup done in {} milliseconds", (currentMillis - startupMillis));
        
        dumpables.forEach( Dumpable::dumpInfo );
    }
}
