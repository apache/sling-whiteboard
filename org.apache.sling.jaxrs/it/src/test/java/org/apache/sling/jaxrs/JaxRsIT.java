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
package org.apache.sling.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class JaxRsIT {

    private static final Logger log = LoggerFactory.getLogger(JaxRsIT.class);

    static SlingClient adminClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() throws ClientException, IOException, InterruptedException {

        URI uri = URI.create(System.getProperty("sling.it.host", "http://localhost:8080"));
        String username = System.getProperty("sling.it.username", "admin");
        String password = System.getProperty("sling.it.password", "admin");

        log.info("Initializing Sling client with host: {} and user: {}", uri, username);
        adminClient = new SlingClient(uri, username, password);
        OsgiConsoleClient osgiConsoleClient = adminClient.adaptTo(OsgiConsoleClient.class);

        if (!adminClient.exists("/apps/system/install/jaxrs.json")) {
            log.info("Running first time setup...");

            log.info("Installing Feature Installer...");
            osgiConsoleClient.installBundle(
                    new File("target/dependency/org.apache.sling.installer.factory.feature.jar"), true);
            ensureLoaded(osgiConsoleClient);
        }

        log.info("Updating features...");
        adminClient.upload(new File("src/test/resources/jaxrs.json"), "application/json",
                "/apps/system/install/jaxrs.json", true, 200, 201);
        adminClient.upload(new File("src/test/resources/sample.json"), "application/json",
                "/apps/system/install/sample.json", true, 200, 201);
        ensureLoaded(osgiConsoleClient);

        log.info("Updating bundles...");
        osgiConsoleClient.installBundle(new File("target/dependency/org.apache.sling.jaxrs.bundle.jar"), true);
        osgiConsoleClient.installBundle(new File("target/dependency/org.apache.sling.jaxrs.sample.jar"), true);
        ensureLoaded(osgiConsoleClient);

    }

    private static void ensureLoaded(OsgiConsoleClient osgiConsoleClient)
            throws IOException, InterruptedException, ClientException {
        boolean featureLoaded = false;
        for (int i = 0; i < 120 && !featureLoaded; i++) {
            try {
                TimeUnit.SECONDS.sleep(1);
                osgiConsoleClient.getOSGiConfiguration("org.apache.aries.jax.rs.whiteboard~test", 200);
                Map<String, Object> bundleStatus = getJsonBody("/system/console/bundles.json", Collections.emptyList());
                String statusString = bundleStatus.get("status").toString();
                if (!statusString.matches("Bundle information: \\d* bundles in total - all \\d* bundles active.")) {
                    throw new ClientException("Invalid bundle status: " + statusString);
                }
                featureLoaded = true;
            } catch (ClientException ce) {
                log.warn("Features not installed: {}", ce.toString());
                osgiConsoleClient.startBundle("org.apache.sling.jaxrs.sample");
                osgiConsoleClient.startBundle("org.apache.sling.jaxrs.bundle");
                TimeUnit.SECONDS.sleep(1);
            }
        }
        if (!featureLoaded) {
            throw new IOException("Features not loaded after 120 seconds");
        }
    }

    private static Map<String, Object> readJsonBody(SlingHttpResponse response) throws IOException {
        return objectMapper.readValue(response.getContent(),
                new TypeReference<Map<String, Object>>() {
                });
    }

    private static Map<String, Object> getJsonBody(String path, List<NameValuePair> params)
            throws ClientException, IOException {
        SlingHttpResponse response = adminClient.doGet(path,
                params, Collections.emptyList(), 200);
        return readJsonBody(response);
    }

    @Test
    void canGetResource() throws ClientException, IOException {
        Map<String, Object> properties = getJsonBody("/test/sling/resource",
                List.of(new BasicNameValuePair("path", "/content")));
        assertEquals("/content", properties.get("path"));
        assertNotNull(properties.get("type"));
    }

    @Test
    void canHandleProblems() throws ClientException, IOException {
        SlingHttpResponse response = adminClient.doGet("/test/sling/resource", List.of(), List.of(), 404);
        assertEquals("application/problem+json", response.getLastHeader("Content-Type").getValue());
        Map<String, Object> properties = readJsonBody(response);
        assertEquals("Not Found", properties.get("title"));
        assertEquals(404, properties.get("status"));
        assertEquals("Resource null not found", properties.get("detail"));
    }

    @Test
    void canGetUserInfo() throws ClientException, IOException {
        Map<String, Object> properties = getJsonBody("/test/userinfo",
                List.of());
        assertEquals("admin", properties.get("userId"));
    }

    @Test
    void canGetSinglePath() throws ClientException, IOException {
        Map<String, Object> properties = getJsonBody("/test/bob",
                List.of());
        assertEquals("The single input was bob (3 characters)", properties.get("message"));
    }

    @Test
    void canGetTwoPaths() throws ClientException, IOException {
        Map<String, Object> properties = getJsonBody("/test/bob/sal",
                List.of());
        assertEquals("The dual input was bob and sal", properties.get("message"));
    }

    @Test
    void canPostBody() throws ClientException, IOException {
        SlingHttpResponse response = adminClient.doPost("/test/name",
                new StringEntity(objectMapper.writeValueAsString(Map.of("name", "Sling"))),
                List.of(new BasicHeader("Content-Type", "application/json")), 200);
        Map<String, Object> properties = readJsonBody(response);
        assertEquals("Hello Sling!", properties.get("message"));
    }
}
