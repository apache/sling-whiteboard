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

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(ServerRule.class)
public class IntegrationTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);

    @Test
    public void connectTimeout() throws IOException {

        SocketTimeoutException exception = assertThrows(SocketTimeoutException.class, 
            () -> assertTimeout(ofSeconds(5),  () -> runTest("http://sling.apache.org:81"))
        );
        assertEquals("connect timed out", exception.getMessage());
    }

    @Test
    public void readTimeout() throws IOException {
        
        SocketTimeoutException exception = assertThrows(SocketTimeoutException.class, 
            () -> assertTimeout(ofSeconds(10),  () -> runTest("http://localhost:" + ServerRule.getLocalPort()))
        );
        assertEquals("Read timed out", exception.getMessage());
    }
    

    private void runTest(String urlSpec) throws MalformedURLException, IOException {
        
        URL url = new URL(urlSpec);
        LOG.info("connecting to {}", url);
        URLConnection connection = url.openConnection();
        // TODO - remove when running through the harness
        connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(3));
        connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(3));
        connection.connect();
        LOG.info("connected");
        try ( InputStream is = connection.getInputStream()) {
            while ( is.read() != -1)
                ;
        }
        LOG.info("read");
    }
}
