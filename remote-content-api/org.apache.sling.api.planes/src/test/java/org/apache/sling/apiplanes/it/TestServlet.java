/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.apiplanes.it;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.servlethelpers.internalrequests.InternalRequest;
import org.osgi.framework.BundleContext;

public class TestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Dictionary<String, Object> properties = new Hashtable<>();
    private static int statusCounter = 1000;
    private final int myStatus = statusCounter++;

    public static final String P_PATHS = "sling.servlet.paths";
    public static final String P_RESOURCE_TYPES = "sling.servlet.resourceTypes";
    public static final String P_EXTENSIONS = "sling.servlet.extensions";
    public static final String P_SELECTORS = "sling.servlet.selectors";
    public static final String P_METHODS = "sling.servlet.methods";
    public static final String RT_DEFAULT = "sling/servlet/default";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendError(myStatus);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    TestServlet with(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    TestServlet register(BundleContext context) {
        context.registerService(Servlet.class.getName(), this, properties);
        return this;
    }

    void assertSelected(InternalRequest forRequest) throws IOException {
        assertEquals(myStatus, forRequest.execute().checkStatus().getStatus());
    }
} 