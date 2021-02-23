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
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.ops4j.pax.exam.junit.PaxExamServer;

import java.net.ServerSocket;
import java.net.URI;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.apache.sling.testing.paxexam.SlingOptions.logback;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;

public class SamlHandlerIT extends TestSupport {

    static int HTTP_PORT = 8080;
    static int DEST_HTTP_PORT = 8484;
    protected static OsgiConsoleClient CLIENT;
    private final static int STARTUP_WAIT_SECONDS = 30;

//    @ClassRule
//    public static PaxExamServer serverRule = new PaxExamServer() {
//        @Override
//        protected void before() throws Exception {
//            // Use a different port for each OSGi framework instance
//            // that's started - they can overlap if the previous one
//            // is not fully stopped when the next one starts.
//            setHttpPort();
//            super.before();
//        }
//    };

//    static void setHttpPort() {
//        try {
//            final ServerSocket serverSocket = new ServerSocket(0);
//            HTTP_PORT = serverSocket.getLocalPort();
//            DEST_HTTP_PORT = serverSocket.getLocalPort();
//            serverSocket.close();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
// systemProperty("org.osgi.service.http.port").value(String.valueOf(HTTP_PORT)),
    @Configuration
    public Option[] configuration() {
        // Patch versions of features provided by SlingOptions
        SlingOptions.versionResolver.setVersionFromProject("org.apache.sling", "org.apache.sling.auth.saml2");
        return new Option[]{
            baseConfiguration(),
            slingQuickstart(),
            logback(),
            // build artifact
            testBundle("bundle.filename"), // from TestSupport
            // testing
            defaultOsgiConfigs(),
            SlingOptions.webconsole(),
            CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.webconsole.plugins.ds", "2.0.8"),
            junitBundles()
        };
    }

    protected Option slingQuickstart() {
        final String workingDirectory = workingDirectory(); // from TestSupport
//        setHttpPort();
        HTTP_PORT = findFreePort();
        DEST_HTTP_PORT = HTTP_PORT+1;
        return composite(
            slingQuickstartOakTar(workingDirectory, HTTP_PORT) // from SlingOptions
        );
    }

    public static Option defaultOsgiConfigs() {
        final String entityID = String.format("http://localhost:%d/", HTTP_PORT);
        final String destinationURL = String.format("http://localhost:%d/auth/realms/sling/protocol/saml", DEST_HTTP_PORT);

        return composite(
            // Configure JAAS
            factoryConfiguration("org.apache.felix.jaas.Configuration.factory")
                .put("jaas.classname", "org.apache.sling.auth.saml2.sp.Saml2LoginModule")
                .put("jaas.controlFlag", "Sufficient")
                .put("jaas.realmName", "jackrabbit.oak")
                .put("jaas.ranking", 110)
                .asOption(),

            // Repo Init
            factoryConfiguration("org.apache.sling.jcr.repoinit.RepositoryInitializer")
                .put("scripts", new String[]{
"create service user saml2-user-mgt\n\nset ACL for saml2-user-mgt\n\nallow jcr:all on /home\n\nend\n\ncreate group sling-authors with path /home/groups/sling-authors"
                })
                .asOption(),

            // Repo Init
            newConfiguration("org.apache.sling.engine.impl.auth.SlingAuthenticator")
                .put("auth.annonymous", false)
                .asOption(),

            // Service User for User Management
            factoryConfiguration("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended")
                .put("user.mapping", new String[]{"org.apache.sling.auth.saml2:Saml2UserMgtService=saml2-user-mgt"})
                .asOption(),

            // SAML Settings
            factoryConfiguration("org.apache.sling.auth.saml2.AuthenticationHandlerSAML2")
                .put("path", "/")
                .put("entityID", entityID)
                .put("acsPath", "/sp/consumer")
                .put("saml2userIDAttr", "urn:oid:0.9.2342.19200300.100.1.1")
                .put("saml2userHome", "/home/users/saml")
                .put("saml2groupMembershipAttr", "urn:oid:2.16.840.1.113719.1.1.4.1.25")
                .put("saml2IDPDestination", destinationURL)
                .put("saml2SPEnabled", true)
                .put("saml2SPEncryptAndSign", false)
                .asOption()
        );
    }

//    @BeforeClass
//    public static void waitForSling() throws Exception {
//        final URI url = new URI(String.format("http://localhost:%d", HTTP_PORT));
//        CLIENT = new OsgiConsoleClient(url, "admin", "admin");
//
//        CLIENT.waitExists("/", STARTUP_WAIT_SECONDS * 1000, 500);
//
//        CLIENT.waitComponentRegistered(AuthenticationHandlerSAML2ImplTest.class.getName(), 10 * 1000, 500);
//
//        // Verify stable status for a bit
//        for(int i=0; i < 10 ; i++) {
//            CLIENT.waitComponentRegistered(AuthenticationHandlerSAML2ImplTest.class.getName(), 1000, 100);
//            Thread.sleep(100);
//        }
//    }

    @Test
    public void exampleTest(){
        assertTrue(true);
    }
}
