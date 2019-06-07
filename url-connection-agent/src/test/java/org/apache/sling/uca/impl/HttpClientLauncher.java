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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * CLI interface to run HTTP clients
 */
public class HttpClientLauncher {
    
    // TODO - write help messages with the values from this enum
    public enum ClientType {
        JavaNet, HC3, HC4
    }

    public static void main(String[] args) throws MalformedURLException, IOException {
        
        if ( args.length != 2 )
            throw new IllegalArgumentException("Usage: java -cp ... " + HttpClientLauncher.class.getName() + " <URL> JavaNet|HC3|HC4");
        
        System.out.println(new Date() + " [WEB] Executing request via " + args[1]);

        switch ( args[1] ) {
            case "JavaNet":
                runUsingJavaNet(args[0]);
                break;
            case "HC3":
                runUsingHttpClient3(args[0]);
                break;
            case "HC4":
                runUsingHttpClient4(args[0]);
                break;
            default:
                throw new IllegalArgumentException("Usage: java -cp ... " + HttpClientLauncher.class.getName() + " <URL> JavaNet|HC3|HC4");
        }
    }

    private static void runUsingJavaNet(String targetUrl) throws MalformedURLException, IOException {
        URLConnection con = new URL(targetUrl).openConnection();
        System.out.println("Connection type is " + con);
        
        try (InputStream in = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ( (line = br.readLine()) != null )
                System.out.println("[WEB] " + line);
        }
    }


    private static void runUsingHttpClient3(String targetUrl) throws HttpException, IOException {
        HttpClient client = new HttpClient();
        // disable retries, to make sure that we get equivalent behaviour with other implementations
        client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
        HttpMethod get = new GetMethod(targetUrl);
        System.out.format("Connection timeouts: connect: %d, so: %s%n", 
                client.getHttpConnectionManager().getParams().getConnectionTimeout(),
                client.getHttpConnectionManager().getParams().getSoTimeout());
        System.out.format("Client so timeout: %d (raw: %s) %n", client.getParams().getSoTimeout(), 
                client.getParams().getParameter(HttpClientParams.SO_TIMEOUT));
        client.executeMethod(get);
        
        System.out.println(new Date() + " [WEB] " + get.getStatusLine());
        
        for ( Header header : get.getResponseHeaders() )
            System.out.print(new Date() + " [WEB] " + header.toExternalForm());
        
        
        try (InputStream in = get.getResponseBodyAsStream()) {
            if (in != null) {
                try (InputStreamReader isr = new InputStreamReader(in); 
                        BufferedReader br = new BufferedReader(isr)) {
                    String line;
                    while ((line = br.readLine()) != null)
                        System.out.println(new Date() + " [WEB] " + line);

                }
            }
        }
    }
    
    private static void runUsingHttpClient4(String targetUrl) throws IOException {
        // disable retries, to make sure that we get equivalent behaviour with other implementations
        try ( CloseableHttpClient client = HttpClients.custom().disableAutomaticRetries().build() ) {
            HttpGet get = new HttpGet(targetUrl);
            try ( CloseableHttpResponse response = client.execute(get)) {
                System.out.println("[WEB] " + response.getStatusLine());
                for ( org.apache.http.Header header : response.getAllHeaders() )
                    System.out.println("[WEB] " + header);
                
                HttpEntity entity = response.getEntity();
                // TODO - print response body
                EntityUtils.consume(entity);
            }
            
        }
    }

}
