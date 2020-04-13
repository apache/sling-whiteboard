/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.auth.saml2;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.auth.saml2.sp.*;
import org.apache.sling.auth.saml2.impl.SAML2ConfigServiceImpl;
import org.apache.sling.auth.saml2.impl.Saml2Credentials;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPRedirectDeflateEncoder;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;



@Component(
        service = AuthenticationHandler.class ,
        name = AuthenticationHandlerSAML2.SERVICE_NAME,
        configurationPid = "org.apache.sling.auth.saml2.impl.SAML2ConfigServiceImpl",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {"sling.servlet.methods={GET, POST}",
            AuthenticationHandler.PATH_PROPERTY+"={}",
            AuthenticationHandler.TYPE_PROPERTY + "=" + AuthenticationHandlerSAML2.AUTH_TYPE,
            "service.description=SAML2 Authentication Handler",
            "service.ranking:Integer=42",
        })

public class AuthenticationHandlerSAML2 extends DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    @Reference
    private SAML2ConfigService saml2ConfigService;
    @Reference
    private Saml2UserMgtService saml2UserMgtService;
    public static final String AUTH_STORAGE_SESSION_TYPE = "session";
    public static final String AUTH_TYPE = "SAML2";
    public static final String TOKEN_FILENAME = "saml2-cookie-tokens.bin";
    private SessionStorage authStorage;
    private long sessionTimeout;
    private static Logger logger = LoggerFactory.getLogger(AuthenticationHandlerSAML2.class);
    static final String SERVICE_NAME = "org.apache.sling.auth.saml2.AuthenticationHandlerSAML2";
    private String[] path;
    private Credential spKeypair;
    private Credential idpVerificationCert;

    /**
     * The request method required for SAML2 submission (value is "POST").
     * POST_BINDING
     */
    private static final String REQUEST_METHOD = "POST";

    /**
     * The factor to convert minute numbers into milliseconds used internally
     */
    private static final long MINUTES = 60L * 1000L;
    private static final long timeoutMinutes = 240; // 4 hr

    /**
     * The {@link TokenStore} used to persist and check authentication data
     */
    private TokenStore tokenStore;

    @Activate @Modified
    protected void activate(ComponentContext componentContext)
            throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, UnsupportedEncodingException {
        this.authStorage = new SessionStorage(SAML2ConfigServiceImpl.AUTHENTICATED_SESSION_ATTRIBUTE);
        this.sessionTimeout = MINUTES * timeoutMinutes;
        this.path = saml2ConfigService.getSaml2Path();

        final File tokenFile = getTokenFile(componentContext.getBundleContext());
        this.tokenStore = new TokenStore(tokenFile, sessionTimeout, false);
//      set encryption keys
        this.spKeypair = KeyPairCredentials.getCredential(
                saml2ConfigService.getJksFileLocation(),
                saml2ConfigService.getJksStorePassword(),
                saml2ConfigService.getSpKeysAlias(),
                saml2ConfigService.getSpKeysPassword());
//      set credential for signing
        this.idpVerificationCert = VerifySignatureCredentials.getCredential(
                saml2ConfigService.getJksFileLocation(),
                saml2ConfigService.getJksStorePassword(),
                saml2ConfigService.getIdpCertAlias());
    }

    private Credential getSpKeypair(){
        return this.spKeypair;
    }
    private Credential getIdpVerificationCert(){
        return this.idpVerificationCert;
    }

    /**
     * Extracts session based credentials from the request. Returns
     * <code>null</code> if the secure user data is not present either in the HTTP Session.
     */
    @Override
    public AuthenticationInfo extractCredentials(final HttpServletRequest httpServletRequest,
                                                 final HttpServletResponse httpServletResponse)  {
// 1. If the request is POST to the ACS URL, it needs to extract the Auth Info from the SAML data POST'ed
        if (saml2ConfigService.getSaml2SPEnabled() ) {
            String reqURI = httpServletRequest.getRequestURI();
            if (reqURI.equals(saml2ConfigService.getAcsPath())){
                doClassloading();
                MessageContext messageContext = decodeHttpPostSamlResp(httpServletRequest);
                boolean relayStateIsOk = validateRelayState(httpServletRequest, messageContext);
                // If relay state from request = relay state from session))
                if (relayStateIsOk) {
                    Response response = (Response) messageContext.getMessage();
                    EncryptedAssertion encryptedAssertion = response.getEncryptedAssertions().get(0);
                    Assertion assertion = decryptAssertion(encryptedAssertion);
                    verifyAssertionSignature(assertion);
                    logger.debug("Decrypted Assertion: ");
                    Helpers.logSAMLObject(assertion);
                    User extUser = doUserManagement(assertion);
                    AuthenticationInfo newAuthInfo = this.buildAuthInfo(extUser);
                    return newAuthInfo;
                }
                return null;
// 2.  try credentials from the session
            } else {
                // Request context is not the ACS path, so get the authInfo from session.
                String authData = authStorage.getString(httpServletRequest);
                if (authData != null) {
                    if (tokenStore.isValid(authData)) {
                        return buildAuthInfo(authData);
                    } else {
                        // clear the token from the session, its invalid and we should get rid of it
                        // so that the invalid cookie isn't present on the authN operation.
                        authStorage.clear(httpServletRequest, httpServletResponse);
                        if ( AuthUtil.isValidateRequest(httpServletRequest)) {
                            // signal the requestCredentials method a previous login failure
                            httpServletRequest.setAttribute(FAILURE_REASON, SamlReason.TIMEOUT);
                            return AuthenticationInfo.FAIL_AUTH;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Requests authentication information from the client.
     * Returns true if the information has been requested and request processing can be terminated normally.
     * Otherwise the authorization information could not be requested.
     *
     * The HttpServletResponse.sendError methods should not be used by the implementation because these responses
     * might be post-processed by the servlet container's error handling infrastructure thus preventing the correct operation of the authentication handler.
     *
     * To convey a HTTP response status the HttpServletResponse.setStatus method should be used.
     *
     * The value of PATH_PROPERTY service registration property value triggering this call is available as the path request attribute.
     * If the service is registered with multiple path values, the value of the path request attribute may be used to implement specific handling.
     *
     * If the REQUEST_LOGIN_PARAMETER request parameter is set only those authentication handlers registered with an authentication type matching the parameter will be considered for requesting credentials through this method.
     *
     * A handler not registered with an authentication type will, for backwards compatibility reasons, always be called ignoring the actual value of the REQUEST_LOGIN_PARAMETER parameter.
     *
     * Parameters:
     * @param httpServletRequest - The request object.
     * @param httpServletResponse - The response object to which to send the request.
     * @returns true if the handler is able to send an authentication inquiry for the given request. false otherwise.
     * @throws IOException - If an error occurs sending the authentication inquiry to the client.
     */
    @Override
    public boolean requestCredentials(final HttpServletRequest httpServletRequest,
                                      final HttpServletResponse httpServletResponse) throws IOException {
        // check the referrer to see if the request is for this handler
        if (!AuthUtil.checkReferer(httpServletRequest, saml2ConfigService.getAcsPath())) {
            // not for this handler, so return
            return false;
        }
        if (saml2ConfigService.getSaml2SPEnabled() ) {
            doClassloading();
            setGotoURLOnSession(httpServletRequest);
            redirectUserForAuthentication(httpServletRequest, httpServletResponse);
            return true;
        }
        return false;
    }

    private void doClassloading(){
        // Classloading
        BundleWiring bundleWiring = FrameworkUtil.getBundle(AuthenticationHandlerSAML2.class).adapt(BundleWiring.class);
        ClassLoader loader = bundleWiring.getClassLoader();
        Thread thread = Thread.currentThread();
        thread.setContextClassLoader(loader);
    }

    private void setGotoURLOnSession(final HttpServletRequest request) {
        SessionStorage sessionStorage = new SessionStorage(SAML2ConfigServiceImpl.GOTO_URL_SESSION_ATTRIBUTE);
        sessionStorage.setString(request , request.getRequestURL().toString());
    }

    private void redirectUserForAuthentication(final HttpServletRequest httpServletRequest,
                                               final HttpServletResponse httpServletResponse) {
        AuthnRequest authnRequest = buildAuthnRequest();
        redirectUserWithRequest(httpServletRequest, httpServletResponse, authnRequest);
    }

    private AuthnRequest buildAuthnRequest() {
        AuthnRequest authnRequest = Helpers.buildSAMLObject(AuthnRequest.class);
        authnRequest.setIssueInstant(new DateTime());
        authnRequest.setDestination(saml2ConfigService.getSaml2IDPDestination());
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        // Entity ID
        authnRequest.setAssertionConsumerServiceURL(saml2ConfigService.getACSURL());
        authnRequest.setID(Helpers.generateSecureRandomId());
        authnRequest.setIssuer(buildIssuer());
        authnRequest.setNameIDPolicy(buildNameIdPolicy());
//        authnRequest.setRequestedAuthnContext(buildRequestedAuthnContext());
        return authnRequest;
    }


    private RequestedAuthnContext buildRequestedAuthnContext() {
        RequestedAuthnContext requestedAuthnContext = Helpers.buildSAMLObject(RequestedAuthnContext.class);
        requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.MINIMUM);
        AuthnContextClassRef passwordAuthnContextClassRef = Helpers.buildSAMLObject(AuthnContextClassRef.class);
        passwordAuthnContextClassRef.setAuthnContextClassRef(AuthnContext.PASSWORD_AUTHN_CTX);
        requestedAuthnContext.getAuthnContextClassRefs().add(passwordAuthnContextClassRef);
        return requestedAuthnContext;
    }

    private Issuer buildIssuer() {
        Issuer issuer = Helpers.buildSAMLObject(Issuer.class);
        issuer.setValue(saml2ConfigService.getEntityID());
        return issuer;
    }

    private NameIDPolicy buildNameIdPolicy() {
        NameIDPolicy nameIDPolicy = Helpers.buildSAMLObject(NameIDPolicy.class);
        nameIDPolicy.setAllowCreate(true);
        nameIDPolicy.setFormat(NameIDType.TRANSIENT);
        return nameIDPolicy;
    }

    private void redirectUserWithRequest(final HttpServletRequest httpServletRequest ,
                     final HttpServletResponse httpServletResponse, final AuthnRequest authnRequest) {
        MessageContext context = new MessageContext();
        context.setMessage(authnRequest);
        SAMLBindingContext bindingContext = context.getSubcontext(SAMLBindingContext.class, true);
        String state = new BigInteger(130, new SecureRandom()).toString(32);
        bindingContext.setRelayState(state);
        setRelayStateOnSession(httpServletRequest, state);
        SAMLPeerEntityContext peerEntityContext = context.getSubcontext(SAMLPeerEntityContext.class, true);
        SAMLEndpointContext endpointContext = peerEntityContext.getSubcontext(SAMLEndpointContext.class, true);
        endpointContext.setEndpoint(getIPDEndpoint());
        SignatureSigningParameters signatureSigningParameters = new SignatureSigningParameters();
        signatureSigningParameters.setSigningCredential(this.getSpKeypair());
        signatureSigningParameters.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        context.getSubcontext(SecurityParametersContext.class, true).setSignatureSigningParameters(signatureSigningParameters);
        HTTPRedirectDeflateEncoder encoder = new HTTPRedirectDeflateEncoder();
        encoder.setMessageContext(context);
        encoder.setHttpServletResponse(httpServletResponse);

        try {
            encoder.initialize();
        } catch (ComponentInitializationException e) {
            throw new RuntimeException(e);
        }

        logger.info("AuthnRequest: ");
        Helpers.logSAMLObject(authnRequest);

        logger.info("Redirecting to IDP");
        try {
            encoder.encode();
        } catch (MessageEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private Endpoint getIPDEndpoint() {
        SingleSignOnService endpoint = Helpers.buildSAMLObject(SingleSignOnService.class);
        endpoint.setBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        endpoint.setLocation(saml2ConfigService.getSaml2IDPDestination());
        return endpoint;
    }


    private MessageContext decodeHttpPostSamlResp(final HttpServletRequest request) {
        HTTPPostDecoder httpPostDecoder = new HTTPPostDecoder();
        ParserPool parserPool = XMLObjectProviderRegistrySupport.getParserPool();
        httpPostDecoder.setParserPool(parserPool);
        httpPostDecoder.setHttpServletRequest(request);
        try {
            httpPostDecoder.initialize();
            httpPostDecoder.decode();
        } catch (MessageDecodingException e) {
            logger.error("MessageDecodingException");
            throw new RuntimeException(e);
        } catch (ComponentInitializationException e) {
            throw new RuntimeException(e);
        }
        return httpPostDecoder.getMessageContext();
    }

    private Assertion decryptAssertion(final EncryptedAssertion encryptedAssertion) {
        // Use SP Private Key to decrypt
        StaticKeyInfoCredentialResolver keyInfoCredentialResolver = new StaticKeyInfoCredentialResolver(this.spKeypair);
        Decrypter decrypter = new Decrypter(null, keyInfoCredentialResolver, new InlineEncryptedKeyResolver());
        decrypter.setRootInNewDocument(true);
        try {
            return decrypter.decrypt(encryptedAssertion);
        } catch (DecryptionException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyAssertionSignature(final Assertion assertion) {
        if (!assertion.isSigned()) {
            logger.error("Halting");
            throw new RuntimeException("The SAML Assertion was not signed!");
        }
        try {
            SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
            profileValidator.validate(assertion.getSignature());
            // use IDP Cert to verify signature
            SignatureValidator.validate(assertion.getSignature(), this.getIdpVerificationCert());
            logger.info("SAML Assertion signature verified");
        } catch (SignatureException e) {
            throw new RuntimeException("SAML Assertion signature problem", e);
        }
    }

    private User doUserManagement(final Assertion assertion) {
        if (assertion.getAttributeStatements() == null ||
                assertion.getAttributeStatements().get(0) == null ||
                assertion.getAttributeStatements().get(0).getAttributes() == null) {
            logger.warn("SAML Assertion Attribute Statement or Attributes was null ");
            return null;
        }
        // start a user object
        Saml2User saml2User = new Saml2User();
        // iterate the attribute assertions
        for (Attribute attribute : assertion.getAttributeStatements().get(0).getAttributes()) {
            if (attribute.getName().equals(saml2ConfigService.getSaml2userIDAttr())) {
                logger.debug("username attr name: " + attribute.getName());
                for (XMLObject attributeValue : attribute.getAttributeValues()) {
                    if ( ((XSString) attributeValue).getValue() != null ) {
                        saml2User.setId( ((XSString) attributeValue).getValue());
                        logger.debug("username value: " + saml2User.getId());
                    }
                }
            } else if (attribute.getName().equals(saml2ConfigService.getSaml2groupMembershipAttr())) {
                logger.debug("group attr name: " + attribute.getName());
                for (XMLObject attributeValue : attribute.getAttributeValues()) {
                    if ( ((XSString) attributeValue).getValue() != null ) {
                        saml2User.addGroupMembership( ((XSString) attributeValue).getValue());
                        logger.debug("managed group {} added: ", ((XSString) attributeValue).getValue());
                    }
                }
            }
        }

        boolean setUpOk = saml2UserMgtService.setUp();
        if (setUpOk) {
            User samlUser = saml2UserMgtService.getOrCreateSamlUser(saml2User);
            saml2UserMgtService.updateGroupMembership(saml2User);
            saml2UserMgtService.cleanUp();
            return samlUser;
        }
        return null;
    }

    private AuthenticationInfo buildAuthInfo(final User user){
        //AUTHENTICATION_INFO_CREDENTIALS
        try {
            AuthenticationInfo authInfo = new AuthenticationInfo(AUTH_TYPE, user.getID());
            authInfo.put("user.jcr.credentials", new Saml2Credentials(user.getID()));
            return authInfo;
        } catch (RepositoryException e) {
            logger.error("failed to build Authentication Info");
            throw new RuntimeException(e);
        }
    }

    private AuthenticationInfo buildAuthInfo(final String authData) {
        //AUTHENTICATION_INFO_CREDENTIALS
        final String userId = getUserId(authData);
        if (userId == null) {
            return null;
        }
        final AuthenticationInfo info = new AuthenticationInfo(AUTH_TYPE, userId);
        info.put("user.jcr.credentials", new Saml2Credentials(userId));
        return info;
    }

    private void setRelayStateOnSession(HttpServletRequest req, String relayState) {
        SessionStorage sessionStorage = new SessionStorage(saml2ConfigService.getSaml2SessionAttr());
        sessionStorage.setString(req, relayState);
    }

    private boolean validateRelayState(HttpServletRequest req, MessageContext messageContext){
        SAMLBindingContext bindingContext = messageContext.getSubcontext(SAMLBindingContext.class, true);
        String reportedRelayState = bindingContext.getRelayState();
        SessionStorage relayStateStore =new SessionStorage(saml2ConfigService.getSaml2SessionAttr());
        String savedRelayState = relayStateStore.getString(req);
        if (savedRelayState == null || savedRelayState.isEmpty()){
            return false;
        } else if (savedRelayState.equals(reportedRelayState)){
            return true;
        }
        return false;
    }

    private void redirectToGotoURL(HttpServletRequest req, HttpServletResponse resp) {
        String gotoURL = (String)req.getSession().getAttribute(SAML2ConfigServiceImpl.GOTO_URL_SESSION_ATTRIBUTE);
        logger.info("Redirecting to requested URL: " + gotoURL);
        try {
            resp.sendRedirect(gotoURL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Drops any credential and authentication details from the request and asks the client to do the same.
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws IOException
     */
    @Override
    public void dropCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        authStorage.clear(httpServletRequest, httpServletResponse);
    }

    /**
     * Called after an unsuccessful login attempt. This implementation makes sure
     * the authentication data is removed either by removing the cookie or by remove
     * the HTTP Session attribute.
     */
    @Override
    public void authenticationFailed(HttpServletRequest request, HttpServletResponse response,
                                     AuthenticationInfo authInfo) {

        /*
         * Note: This method is called if this handler provided credentials which cause
         * a login failure
         */

        // clear authentication data from Cookie or Http Session
        authStorage.clear(request, response);

        // signal the reason for login failure
        request.setAttribute(FAILURE_REASON, SamlReason.INVALID_CREDENTIALS);
    }

    /**
     * Called after successful login with the given authentication info. This
     * implementation ensures the authentication data is set in either the cookie or
     * the HTTP session with the correct security tokens.
     * <p>
     * If no authentication data already exists, it is created. Otherwise if the
     * data has expired the data is updated with a new security token and a new
     * expiry time.
     * <p>
     * If creating or updating the authentication data fails, it is actually removed
     * from the cookie or the HTTP session and future requests will not be
     * authenticated any longer.
     */
    @Override
    public boolean authenticationSucceeded(HttpServletRequest request, HttpServletResponse response,
                                           AuthenticationInfo authInfo) {

        /*
         * Note: This method is called if this handler provided credentials which
         * succeeded login into the repository
         */

        // ensure fresh authentication data
        refreshAuthData(request, response, authInfo);

        final boolean result;
        // only consider a resource redirect if this is a POST request to the ACS URL
        if (REQUEST_METHOD.equals(request.getMethod()) &&
                request.getRequestURI().endsWith(saml2ConfigService.getAcsPath())) {
            redirectToGotoURL(request, response);
            result = true;
        } else {
            // no redirect, hence continue processing
            result = false;
        }
        // no redirect
        return result;
    }


    private String getAuthData(final AuthenticationInfo info) {
        Object data = info.get(SAML2ConfigServiceImpl.AUTHENTICATED_SESSION_ATTRIBUTE);
        if (data instanceof String) {
            return (String) data;
        }
        return null;
    }

    /**
     * Ensures the authentication data is set (if not set yet) and the expiry time
     * is prolonged (if auth data already existed).
     * <p>
     * This method is intended to be called in case authentication succeeded.
     *
     * @param request
     *            The current request
     * @param response
     *            The current response
     * @param authInfo
     *            The authentication info used to successful log in
     */
    private void refreshAuthData(final HttpServletRequest request, final HttpServletResponse response,
                                 final AuthenticationInfo authInfo) {

        // get current authentication data, may be missing after first login
        String authData = getAuthData(authInfo);

        // check whether we have to "store" or create the data
        final boolean refreshCookie = needsRefresh(authData, this.sessionTimeout);

        // add or refresh the stored auth hash
        if (refreshCookie) {
            long expires = System.currentTimeMillis() + this.sessionTimeout;
            try {
                authData = null;
                authData = tokenStore.encode(expires, authInfo.getUser());
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (IllegalStateException e) {
                throw new RuntimeException(e);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            if (authData != null) {
                authStorage.setString(request, authData);
            } else {
                authStorage.clear(request, response);
            }
        }
    }

    /**
     * Refresh the cookie periodically.
     *
     * @param sessionTimeout
     *            time to live for the session
     * @return true or false
     */
    private boolean needsRefresh(final String authData, final long sessionTimeout) {
        boolean updateCookie = false;
        if (authData == null) {
            updateCookie = true;
        } else {
            String[] parts = TokenStore.split(authData);
            if (parts != null && parts.length == 3) {
                long cookieTime = Long.parseLong(parts[1].substring(1));
                if (System.currentTimeMillis() + (sessionTimeout / 2) > cookieTime) {
                    updateCookie = true;
                }
            }
        }
        return updateCookie;
    }

    /**
     * Returns the user id from the authentication data. If the authentication data
     * is a non-<code>null</code> value with 3 fields separated by an @ sign, the
     * value of the third field is returned. Otherwise <code>null</code> is
     * returned.
     * <p>
     * This method is not part of the API of this class and is package private to
     * enable unit tests.
     *
     * @param authData
     * @return
     */
    String getUserId(final String authData) {
        if (authData != null) {
            String[] parts = TokenStore.split(authData);
            if (parts != null) {
                return parts[2];
            }
        }
        return null;
    }

    /**
     * Returns an absolute file indicating the file to use to persist the security
     * tokens.
     * <p>
     * This method is not part of the API of this class and is package private to
     * enable unit tests.
     *
     * @param bundleContext
     *            The BundleContext to use to make an relative file absolute
     * @return The absolute file
     */
    File getTokenFile(final BundleContext bundleContext) {
        File tokenFile = bundleContext.getDataFile(TOKEN_FILENAME);
        if (tokenFile == null) {
            final String slingHome = bundleContext.getProperty("sling.home");
            if (slingHome != null) {
                tokenFile = new File(slingHome, TOKEN_FILENAME);
            } else {
                tokenFile = new File(TOKEN_FILENAME);
            }
        }
        return tokenFile.getAbsoluteFile();
    }
}
