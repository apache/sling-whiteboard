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
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.auth.saml2.Helpers;
import org.apache.sling.auth.saml2.sp.SPCredentials;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.impl.XSStringBuilder;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPSOAP11Decoder;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPSOAP11Encoder;
import org.opensaml.saml.saml2.core.*;
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
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.auth.saml2.sp.ConsumerServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensaml.saml.saml2.encryption.Encrypter;
import java.io.IOException;


import static org.apache.sling.api.servlets.ServletResolverConstants.*;

@Component(
        service = Servlet.class,
        immediate=true,
        property = {
                SLING_SERVLET_PATHS+"=/idp/artifactResolutionService",
                "sling.auth.requirements=-/idp/artifactResolutionService",
                SLING_SERVLET_METHODS+"=[GET,POST]"
        }
)

public class ArtifactResolutionServlet extends SlingAllMethodsServlet {

        private static Logger logger = LoggerFactory.getLogger(ArtifactResolutionServlet.class);
        public static final String IDP_ENTITY_ID = "TestIDP";
        public static final String ARTIFACT_RESOLUTION_SERVICE = "http://localhost:8080/idp/artifactResolutionService";

        @Override
        protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
                logger.info("received artifactResolve:");
// Classloading
                BundleWiring bundleWiring = FrameworkUtil.getBundle(ConsumerServlet.class).adapt(BundleWiring.class);
                ClassLoader loader = bundleWiring.getClassLoader();
                Thread thread = Thread.currentThread();
                thread.setContextClassLoader(loader);

                HTTPSOAP11Decoder decoder = new HTTPSOAP11Decoder();
                decoder.setHttpServletRequest(request);
                try {
                        BasicParserPool parserPool = new BasicParserPool();
                        parserPool.initialize();
                        decoder.setParserPool(parserPool);
                        decoder.initialize();
                        decoder.decode();
                } catch (MessageDecodingException e) {
                        throw new RuntimeException(e);
                } catch (ComponentInitializationException e) {
                        throw new RuntimeException(e);
                }

//Notice: Changed to fix type error. Added Casting to XMLObject.
                Helpers.logSAMLObject( (XMLObject) decoder.getMessageContext().getMessage());
                ArtifactResponse artifactResponse = buildArtifactResponse();

                MessageContext<SAMLObject> context = new MessageContext<SAMLObject>();
                context.setMessage(artifactResponse);

                HTTPSOAP11Encoder encoder = new HTTPSOAP11Encoder();
                encoder.setMessageContext(context);
                encoder.setHttpServletResponse(response);
                try {
                        encoder.prepareContext();
                        encoder.initialize();
                        encoder.encode();
                } catch (MessageEncodingException e) {
                        throw new RuntimeException(e);
                } catch (ComponentInitializationException e) {
                        throw new RuntimeException(e);
                }
        }

        private ArtifactResponse buildArtifactResponse() {

                ArtifactResponse artifactResponse = Helpers.buildSAMLObject(ArtifactResponse.class);

                Issuer issuer = Helpers.buildSAMLObject(Issuer.class);
                issuer.setValue(IDP_ENTITY_ID);
                artifactResponse.setIssuer(issuer);
                artifactResponse.setIssueInstant(new DateTime());
                artifactResponse.setDestination(ConsumerServlet.ASSERTION_CONSUMER_SERVICE);

                artifactResponse.setID(Helpers.generateSecureRandomId());

                Status status = Helpers.buildSAMLObject(Status.class);
                StatusCode statusCode = Helpers.buildSAMLObject(StatusCode.class);
                statusCode.setValue(StatusCode.SUCCESS);
                status.setStatusCode(statusCode);
                artifactResponse.setStatus(status);

                Response response = Helpers.buildSAMLObject(Response.class);
                response.setDestination(ConsumerServlet.ASSERTION_CONSUMER_SERVICE);
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

                artifactResponse.setMessage(response);

                Assertion assertion = buildAssertion();

                signAssertion(assertion);
                EncryptedAssertion encryptedAssertion = encryptAssertion(assertion);

                response.getEncryptedAssertions().add(encryptedAssertion);
                return artifactResponse;
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

        private SubjectConfirmation buildSubjectConfirmation() {
                SubjectConfirmation subjectConfirmation = Helpers.buildSAMLObject(SubjectConfirmation.class);
                subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);

                SubjectConfirmationData subjectConfirmationData = Helpers.buildSAMLObject(SubjectConfirmationData.class);
                subjectConfirmationData.setInResponseTo("Made up ID");
                subjectConfirmationData.setNotBefore(new DateTime().minusDays(2));
                subjectConfirmationData.setNotOnOrAfter(new DateTime().plusDays(2));
                subjectConfirmationData.setRecipient(ConsumerServlet.ASSERTION_CONSUMER_SERVICE);

                subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);

                return subjectConfirmation;
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

        private Conditions buildConditions() {
                Conditions conditions = Helpers.buildSAMLObject(Conditions.class);
                conditions.setNotBefore(new DateTime().minusDays(2));
                conditions.setNotOnOrAfter(new DateTime().plusDays(2));
                AudienceRestriction audienceRestriction = Helpers.buildSAMLObject(AudienceRestriction.class);
                Audience audience = Helpers.buildSAMLObject(Audience.class);
                audience.setAudienceURI(ConsumerServlet.ASSERTION_CONSUMER_SERVICE);
                audienceRestriction.getAudiences().add(audience);
                conditions.getAudienceRestrictions().add(audienceRestriction);
                return conditions;
        }

        private AttributeStatement buildAttributeStatement() {
                AttributeStatement attributeStatement = Helpers.buildSAMLObject(AttributeStatement.class);

                Attribute attributeUserName = Helpers.buildSAMLObject(Attribute.class);

                XSStringBuilder stringBuilder = (XSStringBuilder) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSString.TYPE_NAME);
                XSString userNameValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
                userNameValue.setValue("bob");

                attributeUserName.getAttributeValues().add(userNameValue);
                attributeUserName.setName("username");
                attributeStatement.getAttributes().add(attributeUserName);

                Attribute attributeLevel = Helpers.buildSAMLObject(Attribute.class);
                XSString levelValue = stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
                levelValue.setValue("999999999");

                attributeLevel.getAttributeValues().add(levelValue);
                attributeLevel.setName("telephone");
                attributeStatement.getAttributes().add(attributeLevel);

                return attributeStatement;

        }

}
