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

package org.apache.sling.remotecontentapi.rcaservlet;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonReader;

import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.servlethelpers.internalrequests.ServletInternalRequest;

class ContentProcessor implements JsonProcessor {

    private final ServletResolver servletResolver;

    ContentProcessor(ServletResolver servletResolver) {
        this.servletResolver = servletResolver;
    }

    @Override
    public void process(PipelineContext pc) throws IOException {
        final String jsonResponse = new ServletInternalRequest(servletResolver, pc.resource)
            .withSelectors("s:cagg")
            .execute()
            .getResponseAsString();

        if(!jsonResponse.trim().isEmpty()) {
            try (JsonReader parser = Json.createReader(new StringReader(jsonResponse))) {
                pc.setContent(parser.readObject());
            }
        }
    }
}