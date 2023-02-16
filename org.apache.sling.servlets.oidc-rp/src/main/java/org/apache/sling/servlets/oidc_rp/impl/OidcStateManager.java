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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.nimbusds.oauth2.sdk.id.State;

/**
 * A simplistic OIDC state manager based on Servlet Sessions
 *
 * <p>This manager takes a simple approach by keeping the state and associated values in memory.
 * Not viable for clustered Sling deployments, where a cluster-aware state is needed, e.g.
 * DocumentNodeStore cluster or external state store.</p>
 *
 */
class OidcStateManager {

    private static final String SESSION_ATTRIBUTE_KEY_PREFIX = OidcStateManager.class.getName();
    static final String PARAMETER_NAME_REDIRECT = "redirect";
    static final String PARAMETER_NAME_CONNECTION = "connection";

    private static String attributeKey(State state) {
        return SESSION_ATTRIBUTE_KEY_PREFIX + "." + state.toString();
    }

    private final HttpServletRequest request;

    private OidcStateManager(HttpServletRequest request) {
        this.request = request;
    }

    static OidcStateManager stateFor(HttpServletRequest request) {
        return new OidcStateManager(request);
    }

    public void registerState(State state) {
        HttpSession session = request.getSession();
        session.setAttribute(attributeKey(state), new HashMap<String,String>());
    }

    public void unregisterState(State state) {
        HttpSession session = request.getSession(false);
        if ( session != null )
            session.removeAttribute(attributeKey(state));
    }

    public boolean isValidState(State state) {
        HttpSession session = request.getSession(false);
        if ( session == null )
            return false;

        return session.getAttribute(attributeKey(state)) != null;
    }

    public void putAttribute(State state, String key, String value) {
        String attributeKey = attributeKey(state);
        @SuppressWarnings("unchecked")
        Map<String,String> attributes = (Map<String, String>) request.getSession().getAttribute(attributeKey);
        if ( attributes == null )
            throw new IllegalStateException("State not initialised");
        attributes.put(key, value);
    }

    public Optional<String> getStateAttribute(State state, String key) {
        HttpSession session = request.getSession(false);
        if ( session == null )
            return Optional.empty();

        String attributeKey = attributeKey(state);
        @SuppressWarnings("unchecked")
        Map<String, String> attributes = (Map<String, String>) session.getAttribute(attributeKey);
        if ( attributes == null )
            return Optional.empty();

        return Optional.ofNullable(attributes.get(key));
    }
}
