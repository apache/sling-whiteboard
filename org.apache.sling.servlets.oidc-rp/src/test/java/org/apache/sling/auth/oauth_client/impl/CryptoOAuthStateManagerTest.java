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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.stream.Stream;

import org.apache.sling.auth.oauth_client.impl.CryptoOAuthStateManager;
import org.apache.sling.auth.oauth_client.impl.OAuthState;
import org.apache.sling.commons.crypto.CryptoService;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.nimbusds.oauth2.sdk.id.State;

class CryptoOAuthStateManagerTest {
    
    static class StubCryptoService implements CryptoService {

        @Override
        public String encrypt(String plainText) {
            return Base64.getEncoder().encodeToString(plainText.getBytes(UTF_8));
        }

        @Override
        public String decrypt(String cipherText) {
            return new String(Base64.getDecoder().decode(cipherText), UTF_8);
        }

    }
    
    static Stream<OAuthState> states() {
        return Stream.of(new OAuthState("key1", "conn1", "redir1"), 
                new OAuthState("key2", "conn2", null)
        );
    }

    @ParameterizedTest
    @MethodSource("states")
    void encryptAndDecryptSymmetry(OAuthState state) {
        
        CryptoOAuthStateManager manager = new CryptoOAuthStateManager(new StubCryptoService());
        
        State nimbusState = manager.toNimbusState(state);
        
        assertThat(nimbusState.getValue()).as("generated Nimbus state")
            .doesNotContain(state.connectionName())
            .doesNotContain(state.redirect() != null ? state.redirect(): "null") // workaround for null redirects
            .isNotBlank();
        
        assertThat(manager.toOAuthState(nimbusState)).as("decoded OAuth state")
            .contains(state);
    }
    
    @Test
    void identicalInputsGenerateDifferentOutputs() {
        
        CryptoOAuthStateManager manager = new CryptoOAuthStateManager(new StubCryptoService());
        
        State firstState = manager.toNimbusState(states().findFirst().get());
        State secondState = manager.toNimbusState(states().findFirst().get());
        
        assertThat(firstState.getValue()).as("generated states are different")
            .isNotEqualTo(secondState.getValue());
    }

}
