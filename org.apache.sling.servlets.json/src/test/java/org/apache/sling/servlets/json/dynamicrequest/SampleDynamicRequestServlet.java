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
package org.apache.sling.servlets.json.dynamicrequest;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.servlets.json.DynamicRequestServlet;
import org.apache.sling.servlets.json.annotations.RequestHandler;
import org.apache.sling.servlets.json.annotations.RequestParameter;

public class SampleDynamicRequestServlet extends DynamicRequestServlet {

    @RequestHandler(methods = { "GET" }, path = "/simple")
    public Map<String, Object> simpleHandler() {
        return Map.of("Hello", "World");
    }

    @RequestHandler(methods = { "GET" }, path = "/simple/**")
    public Map<String, Object> globHandler() {
        return Map.of("Hello", "World2");
    }

    @RequestHandler(methods = { "POST" }, path = "/no-response")
    public void noResponse(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
    }

    @RequestHandler(methods = { "GET" }, path = "/with-param")
    public Map<String, Object> withParameter(HttpServletResponse response,
            @RequestParameter(name = "name") String name) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("Hello", name);
        return resp;
    }

    @RequestHandler(methods = { "GET" }, path = "/npe")
    public void npe(HttpServletResponse response,
            @RequestParameter(name = "name") String name) {
        name.length();
    }

}
