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

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.oidchandler.core.configuration.OIDCConfigServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class Handler {

    private static Handler handler = null;
    private static Object monitor = new Object();
    private static final Logger log = LoggerFactory.getLogger(Handler.class);
    private static final String stateValue = "Scope-8cf2f97-91cf-4639-b6c4-682a84a08d30";
    private static final String nonceValue = "Nonce-2aa208-154e-4478-a431-ee6e6184c725";

    private Handler(){

    }

    public static Handler getHandler(){
        if (handler == null) {
            synchronized (monitor) {
                if (handler == null) {
                    handler = new Handler();
                }
            }
        }
        return handler;
    }

    public static String getExpectedState() {
        return stateValue;
    }

    public static String getExpectedNonce() {
        return nonceValue;
    }

    public URI createAuthenticationRequest() throws URISyntaxException, ParseException {
        //read OSGI Configuration
        String oidcScope = OIDCConfigServlet.getOIDCScope();
        String callBackUrl = OIDCConfigServlet.getCallbackURL();
        String authEndpoint = OIDCConfigServlet.getAuthEndpoint();
        String oidcClientID = OIDCConfigServlet.getClientID();

        if (StringUtils.isNoneEmpty(oidcScope, callBackUrl, authEndpoint, oidcClientID)) {
            // Generate random state string for pairing the response to the request
            State state = new State(stateValue);
            // Generate nonce
            Nonce nonce = new Nonce(nonceValue);
            // Specify scope
            Scope scope = Scope.parse(oidcScope);

            //Redirect URI
            URI redirectURI = new URI(callBackUrl);

            //Authorization Endpoint URI
            URI authorizationEndpointURI = new URI(authEndpoint);

            //ClientID
            ClientID clientID = new ClientID(oidcClientID);

            //Prompt value
            Prompt prompt = new Prompt("consent");

            Display display = Display.parse("page");

            // Compose the request
            AuthenticationRequest authenticationRequest = new AuthenticationRequest( authorizationEndpointURI,
                new ResponseType(ResponseType.Value.CODE), null,
                scope, clientID, redirectURI, state, nonce,
                display, prompt, -1, null, null,
                null, null, null, null, null,
                null, null, null);

            URI authReqURI = authenticationRequest.toURI();

            return authReqURI;
        }
        return null;
    }

    public AuthorizationCode getAuthorizationCode(String requestURL){

        AuthenticationResponse authResp = null;
        AuthorizationCode authCode = null;

        try {
            authResp = AuthenticationResponseParser.parse(new URI(requestURL));
        } catch (ParseException | URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
        if (authResp instanceof AuthenticationErrorResponse) {
            ErrorObject error = ((AuthenticationErrorResponse) authResp).getErrorObject();
            log.info(error.getDescription());
        } else {
            AuthenticationSuccessResponse successResponse = (AuthenticationSuccessResponse) authResp;
            // Check state
            if (verifyState(successResponse.getState().toString())) {
                authCode = successResponse.getAuthorizationCode();

            }
        }
        return  authCode;
    }

    public TokenRequest createTokenRequest(AuthorizationCode authCode) throws URISyntaxException {

        //read OSGI Configuration
        String oidcClientSecret = OIDCConfigServlet.getClientSecret();
        String tokenEndpoint = OIDCConfigServlet.getTokenEndpoint();
        String oidcClientID = OIDCConfigServlet.getClientID();
        String callBackUrl = OIDCConfigServlet.getCallbackURL();

        if (StringUtils.isNoneEmpty(oidcClientID, oidcClientSecret, tokenEndpoint, callBackUrl)) {
            //Token Endpoint URI
            URI tokenEndpointURI = new URI(tokenEndpoint);

            //ClientID
            ClientID clientID = new ClientID(oidcClientID);

            //Client Secret
            Secret clientSecret = new Secret(oidcClientSecret);

            //Redirect URI
            URI redirectURI = new URI(callBackUrl);

            TokenRequest tokenReq = new TokenRequest(
                    tokenEndpointURI,
                    new ClientSecretBasic(clientID, clientSecret),
                    new AuthorizationCodeGrant(authCode, redirectURI));
            return tokenReq;

        }
        return null;

    }

    public OIDCTokens getOIDCTokens(HTTPResponse tokenHTTPResp) {

        TokenResponse tokenResponse = null;
        OIDCTokens oidcTokens = null;
        try {
            tokenResponse = OIDCTokenResponseParser.parse(tokenHTTPResp);
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
        }

        if (tokenResponse instanceof TokenErrorResponse) {
            ErrorObject error = ((TokenErrorResponse) tokenResponse).getErrorObject();
            log.info(error.getDescription());
        } else {
            OIDCTokenResponse accessTokenResponse = (OIDCTokenResponse) tokenResponse;
            oidcTokens = accessTokenResponse.getOIDCTokens();
        }

        return  oidcTokens;
    }

    private boolean verifyState(String state){
        if (state.equals(getExpectedState())) {
            return true;
        }
        return false;
    }

}
