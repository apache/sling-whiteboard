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

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ServerRule extends ExternalResource {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServerRule.class);
    
    private Server server;
    
    private int localPort = 12312;

    @Override
    protected void before() throws Throwable {
        server = new Server(localPort);
        ServerConnector connector = new ServerConnector(server) {
            @Override
            public void accept(int acceptorID) throws IOException {
                LOG.info("Waiting before accepting");
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                super.accept(acceptorID);
                LOG.info("Accepted");
            }
        };
        connector.setPort(localPort);
        connector.setConnectionFactories(Collections.singleton(new HttpConnectionFactory() {
            @Override
            public Connection newConnection(Connector connector, EndPoint endPoint) {
                LOG.info("Waiting before creating connection");
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted");
                }
                
                Connection connection = super.newConnection(connector, endPoint);
                LOG.info("Connection created");
                return connection;
            }
        }));
        server.setConnectors(new Connector[] { 
            connector
        });
        server.setHandler(new AbstractHandler() {
            
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                
                LOG.info("Waiting before handling");
                
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                baseRequest.setHandled(true);
                LOG.info("Handled");
            }
        });
        
        server.start();
    }
    
    @Override
    protected void after() {
        if ( server != null )
            try {
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public int getLocalPort() {
        return localPort;
    }
}