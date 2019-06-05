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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

public class Main {

    public static void main(String[] args) throws MalformedURLException, IOException {
        
        if ( args.length != 2 )
            throw new IllegalArgumentException("Usage: java -cp ... " + Main.class.getName() + " <URL> JavaNet|HC3|HC4");

        switch ( args[1] ) {
            case "JavaNet":
                runUsingJavaNet(args[0]);
                break;
            case "HC3":
                runUsingHttpClient3(args[0]);
                break;
            default:
                throw new IllegalArgumentException("Usage: java -cp ... " + Main.class.getName() + " <URL> JavaNet|HC3|HC4");
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
        HttpMethod get = new GetMethod(targetUrl);
        
        client.executeMethod(get);
        
        System.out.println("[WEB] " + get.getStatusLine());
        
        for ( Header header : get.getResponseHeaders() )
            System.out.print("[WEB] " + header.toExternalForm());
        
        
        try (InputStream in = get.getResponseBodyAsStream()) {
            if (in != null) {
                try (InputStreamReader isr = new InputStreamReader(in); 
                        BufferedReader br = new BufferedReader(isr)) {
                    String line;
                    while ((line = br.readLine()) != null)
                        System.out.println("[WEB] " + line);

                }
            }
        }
    }
}
