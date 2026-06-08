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
package org.apache.sling.fmexample.it;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the example bundle - which travelled content-package -&gt; cp2fm feature -&gt; aggregate -&gt;
 * launcher - is installed and ACTIVE on the launched Sling instance.
 */
class ExampleBundleIT {

    private static final String BUNDLE_SYMBOLIC_NAME = "org.apache.sling.fmexample.bundle";
    private static final String AUTH =
            "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));

    private final int port = Integer.getInteger("HTTP_PORT", 8080);
    private final String consoleUrl = "http://localhost:" + port + "/system/console/bundles.json";

    @Test
    void exampleBundleIsActive() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            await().atMost(Duration.ofMinutes(2))
                    .pollInterval(Duration.ofSeconds(3))
                    .ignoreExceptions()
                    .untilAsserted(() -> assertEquals("Active", bundleState(client)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String bundleState(CloseableHttpClient client) throws Exception {
        HttpGet get = new HttpGet(consoleUrl);
        get.setHeader("Authorization", AUTH); // preemptive basic auth for the Felix web console
        try (CloseableHttpResponse response = client.execute(get)) {
            int status = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            String snippet = body.substring(0, Math.min(200, body.length()));
            assertEquals(200, status, "unexpected status, body=" + snippet);
            assertTrue(body.trim().startsWith("{"), "expected JSON, got: " + snippet);

            JsonArray bundles =
                    JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("data");
            for (int i = 0; i < bundles.size(); i++) {
                JsonObject bundle = bundles.get(i).getAsJsonObject();
                if (BUNDLE_SYMBOLIC_NAME.equals(bundle.get("symbolicName").getAsString())) {
                    return bundle.get("state").getAsString();
                }
            }
            return "NOT_PRESENT";
        }
    }
}
