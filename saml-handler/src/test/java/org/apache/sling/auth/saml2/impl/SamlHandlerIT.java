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
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import javax.inject.Inject;
import org.osgi.service.http.HttpService;
import static org.apache.sling.testing.paxexam.SlingOptions.logback;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

/**
 * PAX Exam Tests are a Work in Progress
 * and disabled until proven useful.
 */
//@RunWith(PaxExam.class)
public class SamlHandlerIT extends TestSupport {

    static int HTTP_PORT = 8080;
    static int DEST_HTTP_PORT = 8484;
    private final static int STARTUP_WAIT_SECONDS = 30;

    @Inject
    AuthenticationSupport authenticationSupport;

    @Inject
    protected HttpService httpService;

    @Inject
//    @Filter(value = "(authtype=SAML2)")
    protected AuthenticationHandler samlHandler;


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
//            vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
//            systemTimeout(0),
            baseConfiguration(),
            slingQuickstart(),
            mavenBundle("org.apache.jackrabbit", "oak-jackrabbit-api", "1.32.0"),
            mavenBundle("org.apache.felix", "org.apache.felix.webconsole.plugins.ds", "2.0.8"),
            mavenBundle("org.apache.sling", "org.apache.sling.auth.core", "1.5.0"),
            mavenBundle("org.apache.felix", "org.apache.felix.converter", "1.0.14"),
            mavenBundle("org.apache.sling", "org.apache.sling.auth.form", "1.0.20"),
            testBundle("bundle.filename"), // from TestSupport
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.auth.saml2").versionAsInProject(),
            junitBundles(),
            logback(),
            // build artifact
        };
    }

    protected Option slingQuickstart() {
        final String workingDirectory = workingDirectory(); // from TestSupport
        return composite(
            slingQuickstartOakTar(workingDirectory, HTTP_PORT) // from SlingOptions
        );
    }

//    @Test
//    public void test_setup(){
////        PAX is impossible
//        assertNotNull(samlHandler);
//    }
}
