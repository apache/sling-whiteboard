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

import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Test;

public class EndpointIT extends AbstractEndpointIT {

    @Test
    public void testHelloEndpoint() throws Exception {
        Document document = getDocument("/content/srr/examples/hello.html");
        Assert.assertEquals("We're testing some serious scripting here in Version 2", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World2"));
        Assert.assertTrue(document.body().html().contains("Hello2"));
    }

    @Test
    public void testPrecompiledHelloEndpoint() throws Exception {
        Document document = getDocument("/content/srr/examples/precompiled-hello.html");
        Assert.assertEquals("We're testing some serious scripting here in Version 2", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World2"));
        Assert.assertTrue(document.body().html().contains("Hello2"));
    }

    @Test
    public void testHelloEndpointV1() throws Exception {
        Document document = getDocument("content/srr/examples/hello-v1.html");
        Assert.assertEquals("We're testing some serious scripting here", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World"));
        Assert.assertTrue(document.body().html().contains("Hello"));
    }

    @Test
    public void testPrecompiledHelloEndpointV1() throws Exception {
        Document document = getDocument("content/srr/examples/precompiled-hello-v1.html");
        Assert.assertEquals("We're testing some serious scripting here", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World"));
        Assert.assertTrue(document.body().html().contains("Hello"));
    }

    @Test
    public void testHelloEndpointV2() throws Exception {
        Document document = getDocument("content/srr/examples/hello-v2.html");
        Assert.assertEquals("We're testing some serious scripting here in Version 2", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World2"));
        Assert.assertTrue(document.body().html().contains("Hello2"));
    }

    @Test
    public void testPrecompiledHelloEndpointV2() throws Exception {
        Document document = getDocument("content/srr/examples/precompiled-hello-v2.html");
        Assert.assertEquals("We're testing some serious scripting here in Version 2", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World2"));
        Assert.assertTrue(document.body().html().contains("Hello2"));
    }

    @Test
    public void testHiEndpoint() throws Exception {
        Document document = getDocument("content/srr/examples/hi.html");
        Assert.assertEquals("We're testing some serious scripting here", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World"));
        Assert.assertTrue(document.body().html().contains("Hallo"));
        Assert.assertFalse(document.body().html().contains("Hello"));
    }

    @Test
    public void testHiEndpointV1() throws Exception {
        Document document = getDocument("content/srr/examples/hi-v1.html");
        Assert.assertEquals("We're testing some serious scripting here", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World"));
        Assert.assertTrue(document.body().html().contains("Hallo"));
        Assert.assertFalse(document.body().html().contains("Hello"));
    }

    @Test
    public void testOhHiEndpoint() throws Exception {
        Document document = getDocument("content/srr/examples/ohhi.html");
        Assert.assertEquals("We're testing some serious scripting here", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World"));
        Assert.assertTrue(document.body().html().contains("Ohhi"));
        Assert.assertFalse(document.body().html().contains("Hello"));
    }

    @Test
    public void testOhHiEndpointV1() throws Exception {
        Document document = getDocument("content/srr/examples/ohhi-v1.html");
        Assert.assertEquals("We're testing some serious scripting here", document.select("h2").html());
        Assert.assertTrue(document.body().html().contains("World"));
        Assert.assertTrue(document.body().html().contains("Ohhi"));
        Assert.assertFalse(document.body().html().contains("Hello"));
    }
}
