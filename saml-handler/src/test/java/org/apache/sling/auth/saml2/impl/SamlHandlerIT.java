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
import org.apache.sling.auth.saml2.AuthenticationHandlerSAML2;
import org.apache.sling.auth.saml2.Saml2UserMgtService;
import org.apache.sling.auth.saml2.sp.Saml2LoginModule;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;

import javax.inject.Inject;
import java.net.URI;

import static org.apache.sling.testing.paxexam.SlingOptions.logback;
import static org.apache.sling.testing.paxexam.SlingOptions.slingFsresource;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;


@RunWith(PaxExam.class)
public class SamlHandlerIT extends TestSupport {

    static int HTTP_PORT = 8080;
    static int DEST_HTTP_PORT = 8484;
//    protected static OsgiConsoleClient CLIENT;
//    private final static int STARTUP_WAIT_SECONDS = 30;

    @Inject
    AuthenticationSupport authenticationSupport;

//    @Inject
//    protected AuthenticationHandler samlHandler;



    @Configuration
    public Option[] configuration() {
        // Patch versions of features provided by SlingOptions
        HTTP_PORT = findFreePort();
        DEST_HTTP_PORT = findFreePort();
        SlingOptions.versionResolver.setVersionFromProject("org.apache.sling", "org.apache.sling.auth.saml2");
        SlingOptions.versionResolver.setVersion("commons-codec", "commons-codec", "1.14");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-jackrabbit-api", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-api", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-core", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-commons", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-blob", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "jackrabbit-jcr-commons", "2.20.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-blob-plugins", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-core-spi", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-store-spi", "1.32.0");
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
            baseConfiguration(),
            slingQuickstart(),
            CoreOptions.mavenBundle("org.apache.jackrabbit", "oak-jackrabbit-api", "1.32.0"),
            CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.webconsole.plugins.ds", "2.0.8"),
            CoreOptions.mavenBundle("org.apache.sling", "org.apache.sling.auth.core", "1.5.0"),
            CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.converter", "1.0.14"),
            junitBundles(),
            logback(),
            // build artifact
            testBundle("bundle.filename"), // from TestSupport
        };
    }


    protected Option slingQuickstart() {
        final String workingDirectory = workingDirectory(); // from TestSupport
        return composite(
            slingQuickstartOakTar(workingDirectory, HTTP_PORT) // from SlingOptions
        );
    }


    @Test
    public void test_setup(){
        // still WIP
        assertTrue(true);
//        assertNotNull(samlHandler);
    }


//    @ProbeBuilder
//    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
//        //make sure the needed imports are there.
//        probe.setHeader(Constants.IMPORT_PACKAGE, "*,org.apache.sling.testing.clients.osgi.OsgiConsoleClient.*");
//        return probe;
//    }

//    @BeforeClass
//    public static void waitForSling() throws Exception {
//        final URI url = new URI(String.format("http://localhost:%d", HTTP_PORT));
//        CLIENT = new OsgiConsoleClient(url, "admin", "admin");
//
//        CLIENT.waitExists("/", STARTUP_WAIT_SECONDS * 1000, 500);

//        CLIENT.waitComponentRegistered(MyLameServiceForTestingIT.class.getName(), 10 * 1000, 500);

        // Verify stable status for a bit
//        for(int i=0; i < 10 ; i++) {
//            CLIENT.waitComponentRegistered(MyLameServiceForTestingIT.class.getName(), 1000, 100);
//            Thread.sleep(100);
//        }
//    }
}
