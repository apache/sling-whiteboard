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
import org.apache.sling.auth.saml2.idp.IDPCredentials;
import org.apache.sling.auth.saml2.impl.SAML2ConfigServiceImpl;
import org.apache.sling.auth.saml2.impl.Saml2Credentials;
import org.apache.sling.auth.saml2.sp.SPCredentials;
import org.apache.sling.auth.saml2.sync.Saml2User;
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
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.auth.saml2.sp.SessionStorage;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import static org.apache.sling.auth.saml2.idp.Saml2IDPServlet.TEST_IDP_ENDPOINT;


@Component(
        service = AuthenticationHandler.class ,
        name = AuthenticationHandlerSAML2.SERVICE_NAME,
        configurationPid = "org.apache.sling.auth.saml2.impl.SAML2ConfigServiceImpl",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {"sling.servlet.methods={GET, POST}",
            AuthenticationHandler.PATH_PROPERTY+"={}",
            AuthenticationHandler.TYPE_PROPERTY + "=" + AuthenticationHandlerSAML2.AUTH_TYPE,
            "service.description=SAML2 Authentication Handler",
            "service.ranking=42",
        },
        immediate = true)

public class AuthenticationHandlerSAML2 extends DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    @Reference
    private SAML2ConfigService saml2ConfigService;
    @Reference
    private Saml2UserMgtService saml2UserMgtService;
    public static final String AUTH_STORAGE_SESSION_TYPE = "session";
    public static final String AUTH_TYPE = "SAML2";
    private SessionStorage authStorage;
    private static Logger logger = LoggerFactory.getLogger(AuthenticationHandlerSAML2.class);
    static final String SERVICE_NAME = "org.apache.sling.auth.saml2.AuthenticationHandlerSAML2";
    private String[] path;


    @Activate
    protected void activate() {
        this.path = saml2ConfigService.getSaml2Path();
    }

    /**
     * Extracts session based credentials from the request. Returns
     * <code>null</code> if the secure user data is not present either in the HTTP Session.
     */
    @Override
    public AuthenticationInfo extractCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)  {

        if (saml2ConfigService.getSaml2SPEnabled() ) {
            String reqURI = httpServletRequest.getRequestURI();
            if (reqURI.equals(SAML2ConfigServiceImpl.ASSERTION_CONSUMER_SERVICE_PATH)){
                // If (Request Context = /sp/consumer
                doClassloading();
                MessageContext messageContext = decodeHttpPostSamlResp(httpServletRequest);
                boolean relayStateIsOk = validateRelayState(httpServletRequest, messageContext);
                // If relay state from request = relay state from session))
                if (relayStateIsOk) {
                    Response response = (Response) messageContext.getMessage();

                    EncryptedAssertion encryptedAssertion = response.getEncryptedAssertions().get(0);
                    Assertion assertion = decryptAssertion(encryptedAssertion);
                    verifyAssertionSignature(assertion);
                    logger.info("Decrypted Assertion: ");
                    Helpers.logSAMLObject(assertion);
                    User extUser = doUserManagement(assertion);
                    AuthenticationInfo newAuthInfo = this.buildAuthInfo(extUser);
                    setAuthenticatedSession(httpServletRequest, newAuthInfo);
                    redirectToGotoURL(httpServletRequest, httpServletResponse);
                }
                return null;
            } else if (!httpServletRequest.getRequestURI().equals(TEST_IDP_ENDPOINT)){
                // Request context is not the ACS path, so get the authInfo from session.
                AuthenticationInfo info;
                logger.debug("Getting HTTP {} store with attribute name {}", this.AUTH_STORAGE_SESSION_TYPE, saml2ConfigService.getSaml2SessionAttr());
                // Try getting credentials from the session
                this.authStorage = new SessionStorage(saml2ConfigService.getSaml2SessionAttr());
                // extract credentials
                AuthenticationInfo authData = authStorage.extractAuthenticationInfo(httpServletRequest);

                if (authData != null) {
                    return authData;
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
    public boolean requestCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        if (saml2ConfigService.getSaml2SPEnabled() && !httpServletRequest.getContextPath().equals(TEST_IDP_ENDPOINT)) {
            doClassloading();
            setGotoURLOnSession(httpServletRequest);
            redirectUserForAuthentication(httpServletRequest, httpServletResponse);

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

    private void setGotoURLOnSession(HttpServletRequest request) {
        SessionStorage sessionStorage = new SessionStorage(SAML2ConfigServiceImpl.GOTO_URL_SESSION_ATTRIBUTE);
        sessionStorage.setString(request , request.getRequestURL().toString());
    }

    private void redirectUserForAuthentication(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        AuthnRequest authnRequest = buildAuthnRequest();
        redirectUserWithRequest(httpServletRequest, httpServletResponse, authnRequest);
    }

    private AuthnRequest buildAuthnRequest() {
        AuthnRequest authnRequest = Helpers.buildSAMLObject(AuthnRequest.class);
        authnRequest.setIssueInstant(new DateTime());
        authnRequest.setDestination(saml2ConfigService.getSaml2IDPDestination());
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        // Entity ID
        authnRequest.setAssertionConsumerServiceURL(saml2ConfigService.getACSURL());
        authnRequest.setID(Helpers.generateSecureRandomId());
        authnRequest.setIssuer(buildIssuer());
        authnRequest.setNameIDPolicy(buildNameIdPolicy());
        authnRequest.setRequestedAuthnContext(buildRequestedAuthnContext());

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

    private void redirectUserWithRequest(HttpServletRequest httpServletRequest ,
                                         HttpServletResponse httpServletResponse, AuthnRequest authnRequest) {
        //https://blog.samlsecurity.com/2016/08/signing-and-sending-authnrequests-in.html
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
        signatureSigningParameters.setSigningCredential(SPCredentials.getCredential());
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


    private MessageContext decodeHttpPostSamlResp(HttpServletRequest request) {
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

    private Assertion decryptAssertion(EncryptedAssertion encryptedAssertion) {
        StaticKeyInfoCredentialResolver keyInfoCredentialResolver = new StaticKeyInfoCredentialResolver(SPCredentials.getCredential());
        Decrypter decrypter = new Decrypter(null, keyInfoCredentialResolver, new InlineEncryptedKeyResolver());
        decrypter.setRootInNewDocument(true);
        try {
            return decrypter.decrypt(encryptedAssertion);
        } catch (DecryptionException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyAssertionSignature(Assertion assertion) {
        if (!assertion.isSigned()) {
            throw new RuntimeException("The SAML Assertion was not signed");
        }
        try {
            SAMLSignatureProfileValidator profileValidator = new SAMLSignatureProfileValidator();
            profileValidator.validate(assertion.getSignature());
            SignatureValidator.validate(assertion.getSignature(), IDPCredentials.getCredential());
            logger.info("SAML Assertion signature verified");
        } catch (SignatureException e) {
            e.printStackTrace();
            logger.error("SAML Assertion signature problem", e);
        }
    }

    private User doUserManagement(Assertion assertion) {
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

    private AuthenticationInfo buildAuthInfo(User user){
        try {
            AuthenticationInfo authInfo = new AuthenticationInfo(AuthenticationHandlerSAML2.AUTH_TYPE, user.getID());
            //AUTHENTICATION_INFO_CREDENTIALS
            authInfo.put("user.jcr.credentials", new Saml2Credentials(user.getID()));
            return authInfo;
        } catch (RepositoryException e) {
            logger.error("failed to build Authentication Info");
            throw new RuntimeException(e);
        }
    }

    private void setAuthenticatedSession(HttpServletRequest req, AuthenticationInfo authInfo) {
        req.getSession().setAttribute(SAML2ConfigServiceImpl.AUTHENTICATED_SESSION_ATTRIBUTE, true);
        SessionStorage sessionStorage = new SessionStorage(saml2ConfigService.getSaml2SessionAttr());
        sessionStorage.setAuthInfo(req, authInfo);
    }

    private void setRelayStateOnSession(HttpServletRequest req, String relayState) {
        req.getSession().setAttribute(SAML2ConfigServiceImpl.AUTHENTICATED_SESSION_ATTRIBUTE, true);
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
//TODO: Not Implemented yet
    }

}
