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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);
    
    @Rule
    public ServerRule server = new ServerRule();

    @Test(expected = IOException.class, timeout = 5000)
    public void connectTimeout() throws IOException {

        runTest(false);
    }

    @Test(expected = IOException.class, timeout = 15000)
    public void readTimeout() throws IOException {
        
        runTest(true);
    }
    

    private void runTest(boolean shouldConnect) throws MalformedURLException, IOException {
        URL url = new URL("http://localhost:" + server.getLocalPort());
        LOG.info("connecting");
        URLConnection connection = url.openConnection();
        // TODO - remove when running through the harness
        connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(3));
        connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(3));
        connection.connect();
        /*
         * if ( !shouldConnect ) fail("Connection should not be succesful");
         */        
        LOG.info("connected");
        try ( InputStream is = connection.getInputStream()) {
            while ( is.read() != -1)
                ;
        }
        LOG.info("read");
    }
}
