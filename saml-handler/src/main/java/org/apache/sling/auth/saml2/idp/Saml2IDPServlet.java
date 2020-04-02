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
 *
 */

package org.apache.sling.auth.saml2.idp;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import java.io.Writer;
import org.apache.sling.auth.saml2.Helpers;
import org.apache.sling.auth.saml2.SAML2ConfigService;
import org.apache.sling.auth.saml2.impl.SAML2ConfigServiceImpl;
import org.apache.sling.auth.saml2.sp.SPCredentials;
import org.apache.velocity.app.VelocityEngine;
import org.joda.time.DateTime;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.impl.XSStringBuilder;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.encryption.Encrypter;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.xmlsec.encryption.support.DataEncryptionParameters;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import org.opensaml.xmlsec.encryption.support.EncryptionException;
import org.opensaml.xmlsec.encryption.support.KeyEncryptionParameters;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.LoggerFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import org.slf4j.Logger;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;


@Component(
        service = Servlet.class,
        immediate=true,
        property = {
            SLING_SERVLET_PATHS+"="+Saml2IDPServlet.TEST_IDP_ENDPOINT,
            SLING_SERVLET_METHODS+"=[GET,POST]",
            "sling.auth.requirements=-"+Saml2IDPServlet.TEST_IDP_ENDPOINT
        }
)
public class Saml2IDPServlet extends SlingAllMethodsServlet {
    private static Logger logger = LoggerFactory.getLogger(Saml2IDPServlet.class);
    public static final String IDP_ENTITY_ID = "TestIDP";
    public static final String TEST_IDP_ENDPOINT = "/idp/profile/SAML2/Redirect/SSO";
    @Reference
    private SAML2ConfigService saml2ConfigService;

    @Override
    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws ServletException, IOException {
        logger.info("AuthnRequest received");
        Writer w = resp.getWriter();
        resp.setContentType("text/html");
        w.append("<html>" + "<head></head>" + "<body><h1>You are now at IDP, click the button to authenticate</h1> <form method=\"POST\">"
                + "<input type=\"submit\" value=\"Authenticate\"/>" + "</form>" + "</body>" + "</html>");
    }

    @Override
    protected void doPost(final SlingHttpServletRequest req, final SlingHttpServletResponse resp) throws ServletException, IOException {
        doClassloading();
        // build saml assertion
        Response samlResponse = buildResponse();
        MessageContext<SAMLObject> context = new MessageContext<SAMLObject>();
        String relayState =req.getParameter("RelayState");
        context.getSubcontext(SAMLBindingContext.class, true).setRelayState(relayState);
        SAMLPeerEntityContext peerEntityContext = context.getSubcontext(SAMLPeerEntityContext.class, true);
        SAMLEndpointContext endpointContext = peerEntityContext.getSubcontext(SAMLEndpointContext.class, true);
        endpointContext.setEndpoint(getIPDEndpoint());

        context.setMessage(samlResponse);
        try {
            HTTPPostEncoder encoder = new HTTPPostEncoder();
            VelocityEngine ve = Helpers.getVelocityEngine();
            encoder.setVelocityEngine(ve);
            encoder.setVelocityTemplateId("/templates/saml2-post-binding.vm");
            encoder.setMessageContext(context);
            encoder.setHttpServletResponse(resp);
            encoder.initialize();
            encoder.encode();
        } catch (MessageEncodingException e) {
            throw new RuntimeException(e);
        } catch (ComponentInitializationException e) {
            throw new RuntimeException(e);
        }
    }

    private Endpoint getIPDEndpoint() {
        SingleSignOnService endpoint = Helpers.buildSAMLObject(SingleSignOnService.class);
        endpoint.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
        String entityID = saml2ConfigService.getEntityID();
        entityID = entityID.endsWith("/") ? entityID.substring(0, entityID.length()-1) : entityID;
        endpoint.setLocation(entityID + SAML2ConfigServiceImpl.ASSERTION_CONSUMER_SERVICE_PATH);
        return endpoint;
    }

    private Response buildResponse() {
        Response response = Helpers.buildSAMLObject(Response.class);
        response.setDestination(saml2ConfigService.getACSURL());
        response.setIssueInstant(new DateTime());
        response.setID(Helpers.generateSecureRandomId());

        Issuer issuer2 = Helpers.buildSAMLObject(Issuer.class);
        issuer2.setValue(IDP_ENTITY_ID);
        response.setIssuer(issuer2);

        Status status2 = Helpers.buildSAMLObject(Status.class);
        StatusCode statusCode2 = Helpers.buildSAMLObject(StatusCode.class);
        statusCode2.setValue(StatusCode.SUCCESS);
        status2.setStatusCode(statusCode2);
        response.setStatus(status2);

        Assertion assertion = buildAssertion();
        signAssertion(assertion);
        EncryptedAssertion encryptedAssertion = encryptAssertion(assertion);
        response.getEncryptedAssertions().add(encryptedAssertion);
        return response;
    }

    private Assertion buildAssertion() {
        Assertion assertion = Helpers.buildSAMLObject(Assertion.class);
        Issuer issuer = Helpers.buildSAMLObject(Issuer.class);
        issuer.setValue(IDP_ENTITY_ID);
        assertion.setIssuer(issuer);
        assertion.setIssueInstant(new DateTime());
        assertion.setID(Helpers.generateSecureRandomId());
        Subject subject = Helpers.buildSAMLObject(Subject.class);
        assertion.setSubject(subject);
        NameID nameID = Helpers.buildSAMLObject(NameID.class);
        nameID.setFormat(NameIDType.TRANSIENT);
        nameID.setValue("Some NameID value");
        nameID.setSPNameQualifier("SP name qualifier");
        nameID.setNameQualifier("Name qualifier");
        subject.setNameID(nameID);
        subject.getSubjectConfirmations().add(buildSubjectConfirmation());
        assertion.setConditions(buildConditions());
        assertion.getAttributeStatements().add(buildAttributeStatement());
        assertion.getAuthnStatements().add(buildAuthnStatement());
        return assertion;
    }

    private AuthnStatement buildAuthnStatement() {
        AuthnStatement authnStatement = Helpers.buildSAMLObject(AuthnStatement.class);
        AuthnContext authnContext = Helpers.buildSAMLObject(AuthnContext.class);
        AuthnContextClassRef authnContextClassRef = Helpers.buildSAMLObject(AuthnContextClassRef.class);
        authnContextClassRef.setAuthnContextClassRef(AuthnContext.SMARTCARD_AUTHN_CTX);
        authnContext.setAuthnContextClassRef(authnContextClassRef);
        authnStatement.setAuthnContext(authnContext);
        authnStatement.setAuthnInstant(new DateTime());
        return authnStatement;
    }

    private SubjectConfirmation buildSubjectConfirmation() {
        SubjectConfirmation subjectConfirmation = Helpers.buildSAMLObject(SubjectConfirmation.class);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        SubjectConfirmationData subjectConfirmationData = Helpers.buildSAMLObject(SubjectConfirmationData.class);
        subjectConfirmationData.setInResponseTo("Made up ID");
        subjectConfirmationData.setNotBefore(new DateTime().minusDays(2));
        subjectConfirmationData.setNotOnOrAfter(new DateTime().plusDays(2));
        subjectConfirmationData.setRecipient(saml2ConfigService.getACSURL());
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        return subjectConfirmation;
    }

    private Conditions buildConditions() {
        Conditions conditions = Helpers.buildSAMLObject(Conditions.class);
        conditions.setNotBefore(new DateTime().minusDays(2));
        conditions.setNotOnOrAfter(new DateTime().plusDays(2));
        AudienceRestriction audienceRestriction = Helpers.buildSAMLObject(AudienceRestriction.class);
        Audience audience = Helpers.buildSAMLObject(Audience.class);
        audience.setAudienceURI(saml2ConfigService.getACSURL());
        audienceRestriction.getAudiences().add(audience);
        conditions.getAudienceRestrictions().add(audienceRestriction);
        return conditions;
    }

    private AttributeStatement buildAttributeStatement() {
        // used for every attribute
        AttributeStatement attributeStatement = Helpers.buildSAMLObject(AttributeStatement.class);
        XSStringBuilder stringBuilder = (XSStringBuilder) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSString.TYPE_NAME);
        // used to add username attribute
        Attribute attributeUserName = Helpers.buildSAMLObject(Attribute.class);
        XSString userNameValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        userNameValue.setValue("bob");
        attributeUserName.getAttributeValues().add(userNameValue);
        attributeUserName.setName("username");
        attributeStatement.getAttributes().add(attributeUserName);
        // used to add telephone
        Attribute attributeLevel = Helpers.buildSAMLObject(Attribute.class);
        XSString levelValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        levelValue.setValue("999999999");
        attributeLevel.getAttributeValues().add(levelValue);
        attributeLevel.setName("telephone");
        attributeStatement.getAttributes().add(attributeLevel);
        // used to add group memberships
        Attribute attributeGroups = Helpers.buildSAMLObject(Attribute.class);
        XSString groupsValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        groupsValue.setValue("pcms-authors");
        attributeGroups.getAttributeValues().add(groupsValue);
        attributeGroups.setName("urn:oid:2.16.840.1.113719.1.1.4.1.25");
        attributeGroups.setFriendlyName("groupMembership");
        attributeStatement.getAttributes().add(attributeGroups);
        return attributeStatement;
    }

    private void signAssertion(Assertion assertion) {
        Signature signature = Helpers.buildSAMLObject(Signature.class);
        signature.setSigningCredential(IDPCredentials.getCredential());
        signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        assertion.setSignature(signature);
        try {
            XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
        } catch (MarshallingException e) {
            throw new RuntimeException(e);
        }
        try {
            Signer.signObject(signature);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private EncryptedAssertion encryptAssertion(Assertion assertion) {
        DataEncryptionParameters encryptionParameters = new DataEncryptionParameters();
        encryptionParameters.setAlgorithm(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128);
        KeyEncryptionParameters keyEncryptionParameters = new KeyEncryptionParameters();
        keyEncryptionParameters.setEncryptionCredential(SPCredentials.getCredential());
        keyEncryptionParameters.setAlgorithm(EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP);
        Encrypter encrypter = new Encrypter(encryptionParameters, keyEncryptionParameters);
        encrypter.setKeyPlacement(Encrypter.KeyPlacement.INLINE);
        try {
            EncryptedAssertion encryptedAssertion = encrypter.encrypt(assertion);
            return encryptedAssertion;
        } catch (EncryptionException e) {
            throw new RuntimeException(e);
        }
    }

    private void doClassloading(){
        // Classloading
        BundleWiring bundleWiring = FrameworkUtil.getBundle(Saml2IDPServlet.class).adapt(BundleWiring.class);
        ClassLoader loader = bundleWiring.getClassLoader();
        Thread thread = Thread.currentThread();
        thread.setContextClassLoader(loader);
    }

}
