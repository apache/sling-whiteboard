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
package org.apache.sling.auth.oauth_client.impl;

import java.util.Optional;

import org.apache.sling.commons.crypto.CryptoService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.oauth2.sdk.id.State;

@Component
public class CryptoOAuthStateManager implements OAuthStateManager {

    private static final Logger logger = LoggerFactory.getLogger(CryptoOAuthStateManager.class);
    private final CryptoService cryptoService;

    @Activate
    public CryptoOAuthStateManager(@Reference(target = "(names=sling-oauth)") CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
    
    @Override
    public State toNimbusState(OAuthState state) {
        
        // Generate and encrypt state
        String rawState = state.perRequestKey() + "|" + state.connectionName();
        if ( state.redirect() != null ) {
            rawState += "|" + state.redirect();
        }
        
        return new State(cryptoService.encrypt(rawState));
    }

    @Override
    public Optional<OAuthState> toOAuthState(State state) {
        
        if ( state == null )
            return Optional.empty();
        
        try {
            String encrypted = state.getValue();
            
            String rawState = cryptoService.decrypt(encrypted);
            
            String[] parts = rawState.split("\\|");
            if ( parts.length == 2 )
                return Optional.of(new OAuthState(parts[0], parts[1], null));
            else if ( parts.length == 3) 
                return Optional.of(new OAuthState(parts[0], parts[1], parts[2]));
                        
            logger.warn("Decoded state token does not contain the expected number of parts");
            return Optional.empty();
        } catch (RuntimeException e) {
            logger.warn("Failed to decode state token", e);
            return Optional.empty();
        }
    }
}
