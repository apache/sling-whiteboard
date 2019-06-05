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

import org.apache.sling.uca.impl.ServerRule.MisbehavingServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(ServerRule.class)
public class IntegrationTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);

    /**
     * Validates that connecting to a unaccessible port on an existing port fails with a connect 
     * timeout exception
     * 
     * <p>It is surprisingly hard to simulate a connnection timeout. The most reliable way seems to
     * be to get a firewall to drop packets, but this is very hard to do portably and safely
     * in a unit test. The least bad possible solution is to access an URL that we know will timeout
     * and that is related to us - the Sling website.</p>
     * 
     * @throws IOException various I/O problems 
     */
    @Test
    public void connectTimeout() throws IOException {

        SocketTimeoutException exception = assertThrows(SocketTimeoutException.class, 
            () -> assertTimeout(ofSeconds(5),  () -> runTest("http://sling.apache.org:81"))
        );
        assertEquals("connect timed out", exception.getMessage());
    }

    /**
     * Validates that connecting to a host that delays the response fails with a read timeout
     * 
     * @throws IOException various I/O problems
     */
    @Test
    public void readTimeout(@MisbehavingServer ServerControl server) throws IOException {
        
        SocketTimeoutException exception = assertThrows(SocketTimeoutException.class, 
            () -> assertTimeout(ofSeconds(10),  () -> runTest("http://localhost:" + server.getLocalPort()))
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
