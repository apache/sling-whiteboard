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
package org.apache.sling.scripting.resolver.internal;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.junit.rules.SlingInstanceRule;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.apache.sling.junit.teleporter.customizers.ITCustomizer.BASE_URL_PROP;

public class EndpointIT {

    @ClassRule
    public static final SlingInstanceRule SLING_INSTANCE_RULE = new SlingInstanceRule();

    @BeforeClass
    public static void before() throws Exception {
        SlingClient client = SLING_INSTANCE_RULE.getAdminClient();
        client.waitExists("/content/srr/examples/hello", 20 * 1000, 1000);
        client.waitExists("/content/srr/examples/hello-v1", 20 * 1000, 1000);
        client.waitExists("/content/srr/examples/hello-v2", 20 * 1000, 1000);
        client.waitExists("/content/srr/examples/hi", 20 * 1000, 1000);
        client.waitExists("/content/srr/examples/hi-v1", 20 * 1000, 1000);
    }

    @Test
    public void testHelloEndpoint() throws IOException, InterruptedException {
        Document document = get("content/srr/examples/hello.html", 200);

        Assert.assertEquals("We're testing some serious scripting here in Version 2", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World2"));
        Assert.assertTrue(document.body().html().contains("Hello2"));
    }

    @Test
    public void testHelloEndpointV1() throws IOException, InterruptedException {
        Document document = get("content/srr/examples/hello-v1.html", 200);

        Assert.assertEquals("We're testing some serious scripting here", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World"));
        Assert.assertTrue(document.body().html().contains("Hello"));
    }

    @Test
    public void testHelloEndpointV2() throws IOException, InterruptedException {
        Document document = get("content/srr/examples/hello-v2.html", 200);

        Assert.assertEquals("We're testing some serious scripting here in Version 2", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World2"));
        Assert.assertTrue(document.body().html().contains("Hello2"));
    }

    @Test
    public void testHiEndpoint() throws IOException, InterruptedException {
        Document document = get("content/srr/examples/hi.html", 200);

        Assert.assertEquals("We're testing some serious scripting here", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World"));
        Assert.assertTrue(document.body().html().contains("Hallo"));
        Assert.assertFalse(document.body().html().contains("Hello"));
    }

    @Test
    public void testHiEndpointV1() throws IOException, InterruptedException {
        Document document = get("content/srr/examples/hi-v1.html", 200);

        Assert.assertEquals("We're testing some serious scripting here", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World"));
        Assert.assertTrue(document.body().html().contains("Hallo"));
        Assert.assertFalse(document.body().html().contains("Hello"));
    }

    private Document get(String path, long expected) throws IOException, InterruptedException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(System.getProperty(BASE_URL_PROP, BASE_URL_PROP + "_IS_NOT_SET") + path);
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(1000)
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(1000)
                .build();
        get.setConfig(requestConfig);
        HttpResponse response = client.execute(get);
        Assert.assertNotNull(response);
        Assert.assertEquals(expected, response.getStatusLine().getStatusCode());
        return Jsoup.parse(response.getEntity().getContent(), "UTF-8", System.getProperty(BASE_URL_PROP, BASE_URL_PROP + "_IS_NOT_SET"));
    }
}
