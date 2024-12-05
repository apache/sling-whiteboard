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

/**
 * Information about an OAuth token
 * 
 * <p>This class encapsulated the known information about the token. It allows the client to
 * make decisions based on the possible states.</p>
 */
public class OAuthToken {

    private final TokenState state;
    private final String value;

    public OAuthToken(TokenState state, String value) {
        this.state = state;
        this.value = value;
    }

    public TokenState getState() {
        return state;
    }

    /**
     * Returns the token value
     * 
     * @return the value, in case the {@link #getState() state} is {@code OidcTokenState#VALID}.
     * @throws IllegalStateException in case the {@link #getState() state} is not {@code OidcTokenState#VALID}.
     */
    public String getValue() {
        if ( state != TokenState.VALID )
            throw new IllegalStateException("Can't retrieve a token value when the token state is "  + state);
        return value;
    }
}
