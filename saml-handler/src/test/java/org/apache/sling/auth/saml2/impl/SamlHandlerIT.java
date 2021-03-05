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

package org.apache.sling.auth.saml2.impl;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.saml2.Saml2UserMgtService;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import javax.inject.Inject;

import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.testing.paxexam.SlingOptions.logback;
import static org.apache.sling.testing.paxexam.SlingOptions.slingAuthForm;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.apache.sling.testing.paxexam.SlingOptions.versionResolver;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;

/**
 * PAX Exam Tests are a Work in Progress
 * and disabled until proven useful.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SamlHandlerIT extends TestSupport {

    static int HTTP_PORT = 8080;
    static int DEST_HTTP_PORT = 8484;
    private final static int STARTUP_WAIT_SECONDS = 30;
    private static Logger logger = LoggerFactory.getLogger(SamlHandlerIT.class);

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected ConfigurationAdmin configurationAdmin;

    @Inject
    AuthenticationSupport authenticationSupport;

    @Inject
    HttpService httpService;

    @Filter(value = "(authtype=SAML2)")
    @Inject
    AuthenticationHandler authHandler;

    @Inject
    Saml2UserMgtService saml2UserMgtService;

    @Configuration
    public Option[] configuration() {
        // Patch versions of features provided by SlingOptions
        HTTP_PORT = findFreePort();
        DEST_HTTP_PORT = findFreePort();
        versionResolver.setVersion("commons-codec", "commons-codec", "1.14");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-jackrabbit-api", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-api", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-core-spi", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-commons", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "jackrabbit-jcr-commons", "2.20.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-blob-plugins", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-store-spi", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-core", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-blob", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-store-composite", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-store-document", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-jcr", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-lucene", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-authorization-principalbased", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-query-spi", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-security-spi", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-segment-tar", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.tika", "tika-core", "1.24");
        SlingOptions.versionResolver.setVersion("org.apache.sling", "org.apache.sling.jcr.oak.server", "1.2.10");

        return new Option[]{
            systemProperty("org.osgi.service.http.port").value(String.valueOf(HTTP_PORT)),
//            vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5015"),
//            systemTimeout(0),
            baseConfiguration(),
            slingQuickstart(),
            slingAuthForm(),
            failOnUnresolvedBundles(),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-jackrabbit-api").version(versionResolver),
            mavenBundle("org.apache.jackrabbit", "oak-auth-external", "1.32.0"),
            testBundle("bundle.filename"), // from TestSupport
            // build artifact
            junitBundles(),
            logback(),
        };
    }

    protected Option slingQuickstart() {
        final String workingDirectory = workingDirectory(); // from TestSupport
        return composite(
            slingQuickstartOakTar(workingDirectory, HTTP_PORT) // from SlingOptions
        );
    }

    protected Bundle findBundle(final String symbolicName) {
        for (final Bundle bundle : bundleContext.getBundles()) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                return bundle;
            }
        }
        return null;
    }

    void logBundles() {
        for (final Bundle bundle : bundleContext.getBundles()) {
//            logs to target/test.log
            String active = bundle.getState() == Bundle.ACTIVE ? "active" : ""+bundle.getState();
            logger.info(bundle.getSymbolicName()+":"+bundle.getVersion().toString()+ "state:"+active);
        }
    }

    @Test
    public void test_setup(){
//        PAX is impossible
        assertNotNull(bundleContext);
        assertNotNull(configurationAdmin);
        assertNotNull(authenticationSupport);
        assertNotNull(httpService);
//        assertNotNull(saml2UserMgtService);
//        assertNotNull(authHandler);
        logBundles();
    }
}
