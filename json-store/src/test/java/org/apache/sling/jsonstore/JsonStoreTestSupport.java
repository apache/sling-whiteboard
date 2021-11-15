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
package org.apache.sling.jsonstore;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;

import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExamServer;
import org.ops4j.pax.exam.options.CompositeOption;

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.apache.sling.testing.paxexam.SlingOptions.http;
import static org.apache.sling.testing.paxexam.SlingOptions.paxLoggingLogback;
import static org.apache.sling.testing.paxexam.SlingOptions.webconsole;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;

public class JsonStoreTestSupport extends TestSupport {
    protected static final int httpPort = getHttpPort();
    private static final String CREDENTIALS = new String(Base64.getEncoder().encode("admin:admin".getBytes()));

    public static final String EXTERNAL_TEST_SERVER_PORT = "external.test.server.port";

    static int getHttpPort() {
        final String extPort = System.getProperty(EXTERNAL_TEST_SERVER_PORT);
        if(extPort != null && extPort.length() > 0) {
            return Integer.valueOf(extPort);
        }
        return findFreePort();
    }

    @ClassRule
    public static PaxExamServer serverRule = new ConditionalPaxServer() {
        @Override
        protected boolean isActive() {
            final String extPort = System.getProperty(EXTERNAL_TEST_SERVER_PORT);
            return extPort == null || extPort.length() == 0;
        }
    };

    @Configuration
    public Option[] configuration() {
        return options(
            systemProperty("org.osgi.service.http.port").value(String.valueOf(httpPort)),
            serverBaseConfiguration(),
            http(),
            webconsole(),
            paxLoggingLogback(),
            slingQuickstartOakTar(System.getProperty("sling.workdir"), httpPort),
            testBundle("bundle.filename"),
            storeSupportBundles()
        );
    }

    protected CompositeOption storeSupportBundles() {
        return composite(
            mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-databind").versionAsInProject(),
            mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-annotations").versionAsInProject(),
            mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-core").versionAsInProject(),
            mavenBundle().groupId("com.networknt").artifactId("json-schema-validator").versionAsInProject()
        );
    }

    @Before
    public void waitForSling() throws Exception {
        final URI url = new URI(String.format("http://localhost:%d", httpPort));
        final OsgiConsoleClient client = new OsgiConsoleClient(url, "admin", "admin");

        try {
            client.waitBundleInstalled("org.apache.sling.jsonstore", 10 * 1000, 500);
        } finally {
            client.close();
        }
    }

    protected HttpURLConnection get(String path) throws IOException {
        final URL url = new URL(String.format("http://localhost:%d%s", httpPort, path));
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", String.format("Basic %s", CREDENTIALS));
        return connection;
    }
}