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
package org.apache.sling.oidchandler.core.login;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.oidchandler.core.exception.AuthenticationError;
import org.apache.sling.oidchandler.core.handlers.Handler;
import org.apache.sling.oidchandler.core.handlers.IDTokenHandler;
import org.apache.sling.oidchandler.core.user.UserManagerImpl;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;

@Component(
        service = AuthenticationHandler.class ,
        name = "OIDC Authentication Handler",
        property = {"sling.servlet.methods={GET, POST}", AuthenticationHandler.PATH_PROPERTY+"=/", },
        immediate = true)

public class OIDCAuthenticationHandler extends DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    private final Logger logger = LoggerFactory.getLogger(OIDCAuthenticationHandler.class);

    private static final String AUTHENTICATION_SCHEME_OIDC = "OIDC";

    private String username="";

    private String password ="";

    @Override
    public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response) {
        // extract credentials and return

        String requestScope = request.getParameter("code");

        String requestURI = request.getRequestURI();

        if (StringUtils.isNotEmpty(requestScope) && StringUtils.isEmpty(username)) {

            AuthenticationInfo info = null;
            try {
                info = oidcLogin(request,response);
            } catch (IOException e) {
                logger.info("Error occurred when extracting credentials.");
            }
            if (info != null) {
                return info;
            }
        }

        if (requestURI.equals("/system/sling/logout")) {
            username = "";
            password ="";
        }

        return new AuthenticationInfo(AUTHENTICATION_SCHEME_OIDC, username, password.toCharArray());
    }

    @Override
    public boolean requestCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        try {
            httpServletResponse.getWriter().print("Request");
        } catch (IOException e) {
            logger.info("Error occurred when requesting credentials.");
        }
        return false;
    }

    @Override
    public void dropCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        try {
            httpServletResponse.getWriter().print("Drop");
        } catch (IOException e) {
            logger.info("Error occurred when dropping credentials.");
        }
    }

    protected AuthenticationInfo oidcLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {

            String responseURL = request.getRequestURL().toString()+"?"+request.getQueryString();

            AuthorizationCode authorizationCode = Handler.getHandler().getAuthorizationCode(responseURL);

            if (authorizationCode != null) {

                try {

                    TokenRequest tokenRequest = Handler.getHandler().createTokenRequest(authorizationCode);

                    if (tokenRequest != null) {
                        HTTPResponse httpResponse = tokenRequest.toHTTPRequest().send();
                        OIDCTokens oidcTokens = Handler.getHandler().getOIDCTokens(httpResponse);

                        if (oidcTokens != null) {

                            String expectedNonce = Handler.getHandler().getExpectedNonce();

                            if (StringUtils.isNotEmpty(expectedNonce)) {
                                IDTokenHandler idTokenHandler = new IDTokenHandler();
                                IDTokenClaimsSet claimsSet = idTokenHandler.verifyIdToken(oidcTokens.getIDToken(), expectedNonce);

                                if (claimsSet != null) {

                                    String userID = (String) claimsSet.getClaim("email");

                                    if (StringUtils.isNotEmpty(userID)) {

                                        UserManagerImpl userManager = new UserManagerImpl();
                                        String userPassword = "";
                                        Credentials credentials = new SimpleCredentials(userID, userPassword.toCharArray());
                                        User user = userManager.getUser(credentials);

                                        if (user == null) {
                                            try {
                                                userManager.createUser(userID, userPassword);
                                                logger.info("User "+userID + " is created.\n User "+userID+" successfully logged in.");

                                            } catch (RepositoryException e) {
                                                logger.info("Error occurred when creating a user.");
                                            }
                                        } else {
                                            logger.info("User "+userID+ " already exist.\n User "+userID+" successfully logged in.");

                                        }

                                        username = userID;
                                        password = userPassword;

                                        return new AuthenticationInfo(AUTHENTICATION_SCHEME_OIDC, userID, userPassword.toCharArray());

                                    }

                                } else {
                                    logger.info("ID_Token verification has failed.");
                                    AuthenticationError.sendAuthenticationError(response);
                                }

                            } else {
                                logger.info("Expected nonce is different from the actual nonce value.");
                                AuthenticationError.sendAuthenticationError(response);
                            }
                        }

                    } else {
                        logger.info("Error occurred when creating the token request.");
                        AuthenticationError.sendAuthenticationError(response);

                    }

                } catch (URISyntaxException e) {
                    logger.info("Error occurred when creating tokens.");
                }
            } else {
                logger.info("Error occurred while requesting authorization code from OP.");
                AuthenticationError.sendAuthenticationError(response);
            }

        return null;

    }

}
