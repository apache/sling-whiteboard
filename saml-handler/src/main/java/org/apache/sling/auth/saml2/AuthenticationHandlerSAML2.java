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
import org.apache.sling.auth.saml2.sp.ConsumerServlet;
import org.apache.sling.auth.saml2.sp.SPCredentials;
import org.joda.time.DateTime;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPRedirectDeflateEncoder;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.auth.saml2.sp.SessionStorage;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;



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
    public static final String AUTH_STORAGE_SESSION_TYPE = "session";
    public static final String AUTH_TYPE = "SAML2";
    private SessionStorage authStorage;
    private String getAssertionConsumerEndpoint() { return ConsumerServlet.ASSERTION_CONSUMER_SERVICE; }
    private static Logger logger = LoggerFactory.getLogger(AuthenticationHandlerSAML2.class);
    static final String SERVICE_NAME = "org.apache.sling.auth.saml2.AuthenticationHandlerSAML2";
    private String[] path;

    @Activate
    protected void activate() {
        doClassloading();
        this.path = saml2ConfigService.getSaml2Path();
    }

// Implement AuthenticationHandler
    /**
     * Extracts session based credentials from the request. Returns
     * <code>null</code> if the secure user data is not present either in the HTTP Session.
     */
    @Override
    public AuthenticationInfo extractCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)  {
        if (saml2ConfigService.getSaml2SPEnabled()) {
            AuthenticationInfo info;
            logger.debug("Using HTTP {} store with attribute name {}", this.AUTH_STORAGE_SESSION_TYPE, saml2ConfigService.getSaml2SessionAttr());
            // Try getting credentials from the session
            this.authStorage = new SessionStorage(saml2ConfigService.getSaml2SessionAttr());
            // extract credentials
            AuthenticationInfo authData = authStorage.extractAuthenticationInfo(httpServletRequest);

            if (authData != null) {
                return authData;
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
        if (saml2ConfigService.getSaml2SPEnabled()) {
            doClassloading();
            setGotoURLOnSession(httpServletRequest);
            redirectUserForAuthentication(httpServletResponse);
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
        request.getSession().setAttribute(ConsumerServlet.GOTO_URL_SESSION_ATTRIBUTE, request.getRequestURL().toString());
    }

    private void redirectUserForAuthentication(HttpServletResponse httpServletResponse) {
        AuthnRequest authnRequest = buildAuthnRequest();
        redirectUserWithRequest(httpServletResponse, authnRequest);
    }

    private AuthnRequest buildAuthnRequest() {
        AuthnRequest authnRequest = Helpers.buildSAMLObject(AuthnRequest.class);
        authnRequest.setIssueInstant(new DateTime());
        authnRequest.setDestination(saml2ConfigService.getSaml2IDPDestination());
        authnRequest.setProtocolBinding(SAMLConstants.SAML2_ARTIFACT_BINDING_URI);

        authnRequest.setAssertionConsumerServiceURL(getAssertionConsumerEndpoint());
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
        issuer.setValue(ConsumerServlet.SP_ENTITY_ID);
        return issuer;
    }

    private NameIDPolicy buildNameIdPolicy() {
        NameIDPolicy nameIDPolicy = Helpers.buildSAMLObject(NameIDPolicy.class);
        nameIDPolicy.setAllowCreate(true);
        nameIDPolicy.setFormat(NameIDType.TRANSIENT);
        return nameIDPolicy;
    }

    private void redirectUserWithRequest(HttpServletResponse httpServletResponse, AuthnRequest authnRequest) {
        MessageContext context = new MessageContext();
        context.setMessage(authnRequest);
        SAMLBindingContext bindingContext = context.getSubcontext(SAMLBindingContext.class, true);
        bindingContext.setRelayState("teststate");
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

    /**
     * Drops any credential and authentication details from the request and asks the client to do the same.
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws IOException
     */
    @Override
    public void dropCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {

    }


}
