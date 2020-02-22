/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.commons.metrics.test;

import javax.inject.Inject;

import org.apache.sling.commons.metrics.MetricsService;
import org.apache.sling.commons.metrics.MetricsServiceFactory;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;

import static org.apache.sling.testing.paxexam.SlingOptions.scr;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class MetricsServiceFactoryIT extends TestSupport {

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] configuration() {
        return options(
            baseConfiguration(),
            scr(),
            // Commons Metrics
            testBundle("bundle.filename"),
            mavenBundle().groupId("io.dropwizard.metrics").artifactId("metrics-core").versionAsInProject(),
            junitBundles()
        );
    }

    @Test
    public void nullClass() {
        try {
            MetricsServiceFactory.getMetricsService(null);
            fail("Expecting an Exception");
        } catch (IllegalArgumentException asExpected) {
        }
    }

    @Test
    public void classNotLoadedFromOsgiBundle() {
        try {
            MetricsServiceFactory.getMetricsService(String.class);
            fail("Expecting an Exception");
        } catch (IllegalArgumentException asExpected) {
        }
    }

    @Test
    public void classFromBundle() {
        final MetricsService m = MetricsServiceFactory.getMetricsService(getClass());
        assertNotNull("Expecting a MetricsService", m);
    }

}
