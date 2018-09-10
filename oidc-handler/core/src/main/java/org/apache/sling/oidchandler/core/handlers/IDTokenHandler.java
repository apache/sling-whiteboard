/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.oidchandler.core.handlers;

import com.nimbusds.jose.*;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.oidchandler.core.configuration.OIDCConfigServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

public class IDTokenHandler {

    private static final Logger log = LoggerFactory.getLogger(IDTokenHandler.class);

    public IDTokenClaimsSet verifyIdToken(JWT idToken, String nonce) throws MalformedURLException {
        //read OSGI Configuration
        String issuer = OIDCConfigServlet.getIssuerURL();
        String oidcClientID = OIDCConfigServlet.getClientID();
        String jwksURI = OIDCConfigServlet.getJwksUri();

       if (StringUtils.isNoneEmpty(issuer, oidcClientID, jwksURI)) {
           // The required parameters
           Issuer iss = new Issuer(issuer);
           ClientID clientID = new ClientID(oidcClientID);
           JWSAlgorithm jwsAlg = JWSAlgorithm.RS256;
           URL jwkSetURL = new URL(jwksURI);

           // Create validator for signed ID tokens
           IDTokenValidator validator = new IDTokenValidator(iss, clientID, jwsAlg, jwkSetURL);

           // Set the expected nonce
           Nonce expectedNonce = new Nonce(nonce);

           IDTokenClaimsSet claims = null;

           try {
               claims = validator.validate(idToken, expectedNonce);
           } catch (BadJOSEException e) {
               log.error(e.getMessage(), e);
           } catch (JOSEException e) {
               log.error(e.getMessage(), e);
           }
           return  claims;
       }

       return  null;
    }

}
