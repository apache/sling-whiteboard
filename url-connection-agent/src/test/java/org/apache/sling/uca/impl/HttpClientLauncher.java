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
package org.apache.sling.uca.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.EnumSet;
import java.util.stream.Collectors;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * CLI interface to run HTTP clients
 */
public class HttpClientLauncher {
    
    public enum ClientType {
        JavaNet(HttpClientLauncher::runUsingJavaNet), 
        HC3(HttpClientLauncher::runUsingHttpClient3),
        HC4(HttpClientLauncher::runUsingHttpClient4),
        OkHttp(HttpClientLauncher::runUsingOkHttp);
        
        private final HttpConsumer consumer;

        ClientType(HttpConsumer consumer) {
            this.consumer = consumer;
        }
        
        public HttpConsumer getConsumer() {
            return consumer;
        }
        
        static String pipeSeparatedString() {
            return EnumSet.allOf(ClientType.class).stream()
                .map(ClientType::toString)
                .collect(Collectors.joining("|"));
        }
        
        static ClientType fromString(String value) {
            return EnumSet.allOf(ClientType.class).stream()
                .filter( e -> e.toString().equals(value) )
                .findFirst()
                .orElse(null);
        }
    }
    
    /**
     * A <tt>Consumer</tt> that allows throwing checked exceptions.</p>
     *
     */
    @FunctionalInterface
    interface HttpConsumer {
        void accept(String http, int connectTimeoutSeconds, int readTimeoutSeconds) throws Exception;
    }

    public static void main(String[] args) throws Exception {
        
        if ( args.length < 2 )
            throw new IllegalArgumentException(usage());
        
        ClientType type = ClientType.fromString(args[1]);
        if ( type == null )
            throw new IllegalArgumentException(usage());
        
        log("Executing request via " + type);
        
        int connectTimeout = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int readTimeout = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        
        log("Client API configured timeouts: " + connectTimeout + "/" + readTimeout);
        
        type.consumer.accept(args[0], connectTimeout, readTimeout);
    }

    private static String usage() {
        return "Usage: java -cp ... " + HttpClientLauncher.class.getName() + " <URL> " + ClientType.pipeSeparatedString();
    }
    
    private static void log(String msg, Object... args) {
        System.out.format("[LAUNCHER] " + msg + "%n", args);
    }

    private static void runUsingJavaNet(String targetUrl, int connectTimeoutMillis, int readTimeoutMillis) throws IOException  {
        HttpURLConnection con = (HttpURLConnection) new URL(targetUrl).openConnection();
        log("Connection type is %s", con);
        
        con.setConnectTimeout(connectTimeoutMillis);
        con.setReadTimeout(readTimeoutMillis);
        
        try (InputStream in = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(isr)) {
            
            log(con.getResponseCode() + " " + con.getResponseMessage());

            con.getHeaderFields().forEach( (k, v) -> {
                log(k + " : " + v);
            });
        }
    }


    private static void runUsingHttpClient3(String targetUrl, int connectTimeoutMillis, int readTimeoutMillis) throws IOException {
        HttpClient client = new HttpClient();
        // disable retries, to make sure that we get equivalent behaviour with other implementations
        client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
        
        if ( connectTimeoutMillis != 0 )
            client.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, Integer.valueOf(connectTimeoutMillis));
        if ( readTimeoutMillis != 0 )
            client.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, Integer.valueOf(readTimeoutMillis));
        
        HttpMethod get = new GetMethod(targetUrl);
        log("Connection timeouts: connect: %d, so: %s", 
                client.getHttpConnectionManager().getParams().getConnectionTimeout(),
                client.getHttpConnectionManager().getParams().getSoTimeout());
        log("Client so timeout: %d (raw: %s)", client.getParams().getSoTimeout(), 
                client.getParams().getParameter(HttpClientParams.SO_TIMEOUT));
        client.executeMethod(get);
        
        log(get.getStatusLine().toString());
        
        for ( Header header : get.getResponseHeaders() )
            log(header.toExternalForm());
    }
    
    private static void runUsingHttpClient4(String targetUrl, int connectTimeoutMillis, int readTimeoutMillis) throws IOException {
        // disable retries, to make sure that we get equivalent behaviour with other implementations
        
        Builder config = RequestConfig.custom();
        if ( connectTimeoutMillis != 0 )
            config.setConnectTimeout(connectTimeoutMillis);
        if ( readTimeoutMillis != 0 )
            config.setSocketTimeout(readTimeoutMillis);
        
        try ( CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(config.build())
                .disableAutomaticRetries().build() ) {
            
            HttpGet get = new HttpGet(targetUrl);
            try ( CloseableHttpResponse response = client.execute(get)) {
                log(response.getStatusLine().toString());
                for ( org.apache.http.Header header : response.getAllHeaders() )
                    log(header.toString());
                
                EntityUtils.consume(response.getEntity());
            }
        }
    }

    private static void runUsingOkHttp(String targetUrl, int connectTimeoutSeconds, int readTimeoutSeconds) throws IOException {
        OkHttpClient.Builder clientBuilder = new OkHttpClient().newBuilder();
        if ( connectTimeoutSeconds != 0 )
            clientBuilder.connectTimeout(Duration.ofMillis(connectTimeoutSeconds));
        if ( readTimeoutSeconds != 0 )
            clientBuilder.readTimeout(Duration.ofMillis(readTimeoutSeconds));
        
        OkHttpClient client = clientBuilder.build();
        
        Request request = new Request.Builder()
            .url(targetUrl)
            .build();

        try (Response response = client.newCall(request).execute()) {
            log("%s %s", response.code(), response.message());
            response.headers().toMultimap().forEach( (n, v) -> {
                log("%s : %s", n, v);
            });
        }
    }
}
