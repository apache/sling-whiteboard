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

package org.apache.sling.scripting.gql.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.sling.api.SlingHttpServletRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class RequestParser {

    private static final String MIME_TYPE_JSON = "application/json";

    private String query;

    private Map<String, Object> variables;

    // As per GraphQL spec, nulls must be present in the JSON output
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    RequestParser(SlingHttpServletRequest request) throws IOException {
        parse(request);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getInputJson(SlingHttpServletRequest req) throws IOException {
        return GSON.fromJson(req.getReader(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private void parse(SlingHttpServletRequest request) throws IOException {
        if (request.getMethod().equalsIgnoreCase("POST")) {
            if (MIME_TYPE_JSON.equals(request.getContentType())) {
                Map<String, Object> requestJson = getInputJson(request);
                query = (String) requestJson.get("query");
                if (query != null) {
                    query = query.replace("\\n", "\n");
                }
                variables = (Map<String, Object>) requestJson.get("variables");
            }
        }

        if (query == null) {
            query = request.getParameter("query");
        }

        if (variables == null) {
            variables = Collections.emptyMap();
        }
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

}
