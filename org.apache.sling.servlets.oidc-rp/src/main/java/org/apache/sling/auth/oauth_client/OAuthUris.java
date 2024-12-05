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
package org.apache.sling.auth.oauth_client;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.auth.oauth_client.impl.OAuthEntryPointServlet;

public abstract class OAuthUris {

    /**
     * Generates a local URI to the OIDC entry point servlet
     * 
     * <p>The URI can be used as-is to send a redirect to the user and start the OIDC flow.</p>
     * 
     * @param connection The connection to start the OIDC flow for
     * @param request The current request
     * @param redirectPath The local redirect path to use after completing the OIDC flow
     * @return a local URI
     */
    public static URI getOidcEntryPointUri(ClientConnection connection, SlingHttpServletRequest request, String redirectPath) {
        StringBuilder uri = new StringBuilder();
        uri.append(request.getScheme()).append("://").append(request.getServerName());
        boolean needsExplicitPort = ( "https".equals(request.getScheme()) && request.getServerPort() != 443 )
                || ( "http".equals(request.getScheme()) && request.getServerPort() != 80 ) ;
                
        if ( needsExplicitPort ) {
            uri.append(':').append(request.getServerPort());
        }
        uri.append(OAuthEntryPointServlet.PATH).append("?c=").append(connection.name());
        if ( redirectPath != null )
            uri.append("&redirect=").append(URLEncoder.encode(redirectPath, StandardCharsets.UTF_8));

        return URI.create(uri.toString());
    }
    
    private OAuthUris() {
        // prevent instantiation
    }
}
