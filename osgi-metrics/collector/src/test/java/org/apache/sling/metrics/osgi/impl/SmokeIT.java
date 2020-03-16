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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.felix.systemready.SystemReady;
import org.apache.sling.metrics.osgi.StartupMetrics;
import org.apache.sling.metrics.osgi.StartupMetricsListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

@RunWith(PaxExam.class)
public class SmokeIT {

    @Inject
    private BundleContext bc;
    
    @Configuration
    public Option[] config() {
        return options(
            // lower timeout, we don't have
            frameworkProperty(StartupTimeCalculator.PROPERTY_READINESS_DELAY).value("100"),
            bundle("reference:file:target/classes"),
            junitBundles(),
            mavenBundle("org.apache.felix", "org.apache.felix.systemready", "0.4.2"),
            mavenBundle("org.apache.felix", "org.apache.felix.rootcause", "0.1.0"),
            mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.1.16"),
            mavenBundle("org.osgi", "org.osgi.util.promise", "1.1.1"),
            mavenBundle("org.osgi", "org.osgi.util.function", "1.1.0")
        );
    }
    
    @Test
    public void registerListenerAfterSystemIsReady() throws InterruptedException {
        runBasicTest(false);
    }

    @Test
    public void registerListenerBeforeSystemIsReady() throws InterruptedException {
        runBasicTest(true);
    }

    private void runBasicTest(boolean registerListenerFirst) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        DebugListener listener = new DebugListener(latch);
        
        Runnable foo = () -> {};
        
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, "some.service.pid");
        
        ServiceRegistration<Runnable> reg = bc.registerService(Runnable.class, foo, props);
        reg.unregister();
        reg = bc.registerService(Runnable.class, foo, props);
        reg.unregister();

        if ( registerListenerFirst ) {
            bc.registerService(SystemReady.class, new SystemReady() {}, null);
            bc.registerService(StartupMetricsListener.class, listener, null);
        } else {
            bc.registerService(StartupMetricsListener.class, listener, null);
            bc.registerService(SystemReady.class, new SystemReady() {}, null);
        }
        
        latch.await();
        
        StartupMetrics metrics = listener.getMetrics();
        
        assertThat(metrics, notNullValue());
        Set<String> trackedBundleNames = metrics.getBundleStartDurations().stream()
                .map( bsd -> bsd.getSymbolicName())
                .collect(Collectors.toSet());
        Set<String> expectedBundleNames = new HashSet<>();
        expectedBundleNames.add("org.apache.felix.systemready");
        expectedBundleNames.add("org.apache.felix.rootcause");
        expectedBundleNames.add("org.apache.felix.scr");
        expectedBundleNames.add("org.osgi.util.promise");
        expectedBundleNames.add("org.osgi.util.function");
        
        assertTrue("Tracked bundle names " + trackedBundleNames + " did not contain " + expectedBundleNames, 
            trackedBundleNames.containsAll(expectedBundleNames));
        
        assertThat("Service restarts", metrics.getServiceRestarts().size(), equalTo(1));
        assertThat("Restarted component service identifier", metrics.getServiceRestarts().get(0).getServiceIdentifier(), equalTo(Constants.SERVICE_PID+"="+props.get(Constants.SERVICE_PID)));
    }

    static class DebugListener implements StartupMetricsListener {
        
        private CountDownLatch latch;
        private StartupMetrics metrics;

        public DebugListener(CountDownLatch latch) {
            this.latch = latch;
        }
        
        @Override
        public void onStartupComplete(StartupMetrics metrics) {
            this.metrics = metrics;
            latch.countDown();
        }

        public StartupMetrics getMetrics() {
            return metrics;
        }
        
    }
}
