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
package org.apache.sling.extensions.oidc_rp;

import java.net.URI;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * A client for dealing with over-the-network OIDC concerns
 * 
 * <p>This client is able to generate URLs and make network calls related to OIDC.</p>
 * 
 */
public interface OidcClient {

    /**
     * Generates a local URI to the OIDC entry point servlet
     * 
     * <p>The URI can be used as-is to send a redirect to the user and start the OIDC flow.</p>
     * 
     * @param connection The connection to start the OIDC flow for
     * @param request The current request
     * @param redirectPath The local redirect path to use after completing the OIDC flow
     * @return a local URI
     * @throws OidcException in case anything goes wrong
     */
    URI getOidcEntryPointUri(OidcConnection connection, SlingHttpServletRequest request, String redirectPath) throws OidcException;
    
    /**
     * Generates a URI to the OIDC provider's authorization endpoint
     * 
     * <p>The URI can be used as-is to start the OIDC flow directly on the identity provider's side.</p>
     * 
     * @param connection The connection to start the OIDC flow for
     * @param request The current request
     * @param redirectUri The redirect path to use after completing the OIDC flow
     * @return a remote URI
     * @throws OidcException in case anything goes wrong
     */
    URI getAuthenticationRequestUri(OidcConnection connection, SlingHttpServletRequest request, URI redirectUri) throws OidcException;
    
    // OidcTokens getOidcTokens(OidcConnection connection, String authenticationCode) throws OidcException;
    
    /**
     * Refreshes the OIDC tokens based on the supplied refresh token
     * 
     * <p>It is the responsibility of the invoker to persist the returned tokens.</p> 
     * 
     * @param connection The connection to start the OIDC flow for
     * @param refreshToken An existing refresh token
     * @return OIDC tokens
     * @throws OidcException in case anything goes wrong
     */
    OidcTokens refreshTokens(OidcConnection connection, String refreshToken) throws OidcException;
}
