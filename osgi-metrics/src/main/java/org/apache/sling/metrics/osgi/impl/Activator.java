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

import java.util.Hashtable;

import org.apache.sling.metrics.osgi.StartupMetrics;
import org.apache.sling.metrics.osgi.StartupMetricsListener;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
// avoid dependency to SCR so we can start early on
public class Activator implements BundleActivator {

    private BundleStartTimeCalculator bstc;
    private StartupTimeCalculator stc;
    private ServiceRestartCountCalculator srcc;
    private ServiceRegistration<StartupMetricsListener> logger;

    @Override
    public void start(BundleContext context) throws Exception {

        // TODO - to move to separate class or bundle
        logger = context.registerService(StartupMetricsListener.class, new StartupMetricsListener() {
            @Override
            public void onStartupComplete(StartupMetrics event) {
                Logger log = LoggerFactory.getLogger(getClass());
                log.info("Application startup completed in {}", event.getStartupTime());
                event.getBundleStartDurations().forEach( bsd -> log.info("Bundle {} started in {}", bsd.getSymbolicName(), bsd.getStartedAfter()));
                event.getServiceRestarts().stream()
                    .filter( src -> src.getServiceRestarts() > 0)
                    .forEach( src -> log.info("Service identified with {} was restarted {} times", src.getServiceIdentifier(), src.getServiceRestarts()));
            }
        }, new Hashtable<>());
        
        bstc = new BundleStartTimeCalculator(context.getBundle().getBundleId());
        context.addBundleListener(bstc);

        srcc = new ServiceRestartCountCalculator();
        context.addServiceListener(srcc);

        stc = new StartupTimeCalculator(context, bstc, srcc);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        stc.close();
        context.removeServiceListener(srcc);
        context.removeBundleListener(bstc);
        logger.unregister();
    }

}
