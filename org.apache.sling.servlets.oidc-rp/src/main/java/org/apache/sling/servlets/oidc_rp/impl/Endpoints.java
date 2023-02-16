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
package org.apache.sling.servlets.oidc_rp.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

record Endpoints(String authorizationEndpoint, String tokenEndpoint) {

    private static final Logger logger = LoggerFactory.getLogger(Endpoints.class);

    // TODO - cache endpoints, move to service?
    // TODO - can we reuse code from the Nimbus SDK?
    static Endpoints discover(String base, HttpClient client) throws IOException, InterruptedException {
        logger.info("Initiating discovery request for baseUrl {}", base);
        HttpRequest discoveryRequest = HttpRequest.newBuilder()
                .uri(URI.create(base + "/.well-known/openid-configuration"))
                .build();

        HttpResponse<byte[]> response = client.send(discoveryRequest, BodyHandlers.ofByteArray());
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(response.body()))) {
            JsonObject cfg = reader.readObject();
            String authEndpoint = cfg.getJsonString("authorization_endpoint").getString();
            String tokenEndpoint = cfg.getJsonString("token_endpoint").getString();
            Endpoints ep = new Endpoints(authEndpoint, tokenEndpoint);
            logger.info("Discovered auth_endpoint {} , token_endpoint {}", ep.authorizationEndpoint(), ep.tokenEndpoint());
            return ep;
        }
    }
}