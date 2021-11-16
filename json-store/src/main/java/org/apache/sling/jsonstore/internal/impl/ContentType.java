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

package org.apache.sling.jsonstore.internal.impl;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ContentType {
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";

    public static boolean check(HttpServletRequest request, HttpServletResponse response, String expectedContentType) throws IOException {
        String h = request.getHeader(CONTENT_TYPE_HEADER);
        final int commaPos = h == null ? -1 : h.indexOf(";");
        if(commaPos > 0) {
            h = h.substring(0, commaPos).trim();
        }
        if(!expectedContentType.equals(h)) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                String.format("Expected %s=%s, got %s", CONTENT_TYPE_HEADER, expectedContentType, h));
            return false;                
        }
        return true;
    }

    public static boolean checkJson(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return check(request, response, APPLICATION_JSON);
    }
}