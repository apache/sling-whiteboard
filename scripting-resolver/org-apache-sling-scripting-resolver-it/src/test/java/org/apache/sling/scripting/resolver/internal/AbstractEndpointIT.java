/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.resolver.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.sling.junit.teleporter.customizers.ITCustomizer;
import org.apache.sling.testing.junit.rules.SlingInstanceRule;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;


public abstract class AbstractEndpointIT {

    protected int contentFindTimeout = 20000;
    protected int contentFindRetryDelay = 1000;
    private static CloseableHttpClient httpClient;

    @ClassRule
    public static final SlingInstanceRule SLING_INSTANCE_RULE = new SlingInstanceRule();

    private Map<String, Document> documentMap = new ConcurrentHashMap<>();

    @BeforeClass
    public static void setUp() {
        httpClient = HttpClientBuilder.create().build();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        httpClient.close();
    }

    /**
     * Retrieves a jsoup Document from the passed {@code url}. The URL can contain selectors and extensions, but it has to identify a Sling
     * content {@link org.apache.sling.api.resource.Resource}.
     *
     * @param url the URL from which to retrieve the {@link Document}
     * @return the Document
     * @throws Exception if the resource was not found before the timeout elapsed
     */
    protected Document getDocument(String url) throws Exception {
        return getDocument(url, HttpGet.METHOD_NAME);
    }

    protected Document getDocument(String url, String httpMethod, NameValuePair... parameters) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(url);
        uriBuilder.setParameters(parameters);
        URI uri = uriBuilder.build();
        Document document = documentMap.get(httpMethod + ":" + uri.toString());
        if (document == null) {
            HttpResponse response = getResponse(httpMethod, url, 200);
            document = Jsoup.parse(response.getEntity().getContent(), StandardCharsets.UTF_8.name(),
                    System.getProperty(ITCustomizer.BASE_URL_PROP, ITCustomizer.BASE_URL_PROP +
                            "_IS_NOT_SET"));
            documentMap.put(httpMethod + ":" + uri, document);
        }
        return document;
    }

    protected HttpResponse getResponse(String method, String url, int statusCode, NameValuePair... parameters) throws Exception {
        String resourcePath = url.substring(0, url.indexOf('.'));
        SLING_INSTANCE_RULE.getAdminClient().waitExists(resourcePath, contentFindTimeout, contentFindRetryDelay);
        HttpUriRequest request = prepareRequest(method, url, parameters);
        HttpResponse response = httpClient.execute(request);
        Assert.assertNotNull(response);
        Assert.assertEquals("URL " + url + " did not return a " + statusCode + " status code.", statusCode,
                response.getStatusLine().getStatusCode
                        ());
        return response;
    }

    protected HttpUriRequest prepareRequest(String method, String url, NameValuePair... parameters) throws URISyntaxException {
        HttpRequestBase request = null;
        URIBuilder uriBuilder =
                new URIBuilder(System.getProperty(ITCustomizer.BASE_URL_PROP, ITCustomizer.BASE_URL_PROP + "_IS_NOT_SET") + url);
        uriBuilder.setParameters(parameters);
        switch (method) {
            case HttpGet.METHOD_NAME:
                request = new HttpGet(uriBuilder.build());
                break;
            case HttpHead.METHOD_NAME:
                request = new HttpHead(uriBuilder.build());
                break;
            case HttpOptions.METHOD_NAME:
                request = new HttpOptions(uriBuilder.build());
                break;
            case HttpPost.METHOD_NAME:
                request = new HttpPost(uriBuilder.build());
                break;
            case HttpPut.METHOD_NAME:
                request = new HttpPut(uriBuilder.build());
                break;
            case HttpPatch.METHOD_NAME:
                request = new HttpPatch(uriBuilder.build());
                break;
            case HttpTrace.METHOD_NAME:
                request = new HttpTrace(uriBuilder.build());
                break;
            case HttpDelete.METHOD_NAME:
                request = new HttpDelete(uriBuilder.build());
                break;
        }
        return request;
    }

}
