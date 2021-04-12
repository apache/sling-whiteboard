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

package org.apache.sling.auth.saml2.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.saml2.Helpers;
import org.apache.sling.auth.saml2.SAML2RuntimeException;
import org.apache.sling.auth.saml2.Saml2User;
import org.apache.sling.auth.saml2.Saml2UserMgtService;
import org.apache.sling.auth.saml2.sp.KeyPairCredentials;
import org.apache.sling.auth.saml2.sp.VerifySignatureCredentials;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.osgi.framework.BundleContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Dictionary;
import java.util.Hashtable;
import static org.apache.sling.auth.core.spi.AuthenticationHandler.REQUEST_LOGIN_PARAMETER;
import static org.apache.sling.auth.saml2.Activator.initializeOpenSaml;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class OsgiSamlTest {

    @Rule
    public final OsgiContext osgiContext = new OsgiContext();
    BundleContext bundleContext;
    Saml2UserMgtService userMgtService;
    AuthenticationHandlerSAML2Impl samlHandler;
    AuthenticationHandlerSAML2Impl saml2handlerJKS;
    XMLObjectBuilder<XSString> valueBuilder;
    static KeyStore testKeyStore;

    @BeforeClass
    public static void initializeOpenSAML(){
        try {
            initializeOpenSaml();
            testKeyStore = JKSHelper.createExampleJks();
            JKSHelper.addTestingCertsToKeystore(testKeyStore);
        } catch (InitializationException e) {
            fail(e.getMessage());
        }

    }

    @Before
    public void setup(){
        valueBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilderOrThrow(XSString.TYPE_NAME);
        try {
            bundleContext = MockOsgi.newBundleContext();
            ResourceResolverFactory mockFactory = Mockito.mock(ResourceResolverFactory.class);
            osgiContext.registerService(ResourceResolverFactory.class, mockFactory);
            userMgtService = osgiContext.registerService(new Saml2UserMgtServiceImpl());
            samlHandler = osgiContext.registerInjectActivateService(new AuthenticationHandlerSAML2Impl());
            setup_saml2handlerJKS();
        } catch (Exception e){
            fail(e.getMessage());
        }
    }

    @Test
    public void test_default_configs() {
        assertNotNull(samlHandler);
        assertEquals("{}",samlHandler.getSaml2Path());
        assertFalse(samlHandler.getSaml2SPEnabled());
        assertEquals("username",samlHandler.getSaml2userIDAttr());
        assertEquals("http://localhost:8080/",samlHandler.getEntityID());
        assertEquals("http://localhost:8080/sp/consumer",samlHandler.getACSURL());
        assertEquals("/sp/consumer",samlHandler.getAcsPath());
        assertEquals("/home/users/saml",samlHandler.getSaml2userHome());
        assertEquals(null,samlHandler.getSaml2groupMembershipAttr());
        assertTrue(samlHandler.getSyncAttrs().length == 0);
        assertEquals("saml2AuthInfo",samlHandler.getSaml2SessionAttr());
        assertEquals("http://localhost:8080/idp/profile/SAML2/Redirect/SSO", samlHandler.getSaml2IDPDestination());
        assertEquals("https://sling.apache.org/", samlHandler.getSaml2LogoutURL());
        assertFalse(samlHandler.getSaml2SPEncryptAndSign());
        assertEquals(null,samlHandler.getJksFileLocation());
        assertEquals(null,samlHandler.getJksStorePassword());
        assertEquals(null,samlHandler.getIdpCertAlias());
        assertEquals(null,samlHandler.getSpKeysAlias());
        assertEquals(null,samlHandler.getSpKeysPassword());
    }

    @Test
    public void test_not_ignored_when_saml2_specified(){
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter(REQUEST_LOGIN_PARAMETER)).thenReturn("SAML2");
        assertFalse(samlHandler.ignoreRequestCredentials(request));
    }

    @Test
    public void test_disabled_saml_handler(){
        assertFalse(samlHandler.getSaml2SPEnabled());
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        assertNull(samlHandler.extractCredentials(request,response));
        try{
            assertFalse(samlHandler.requestCredentials(request,response));
        } catch (IOException e){
            fail(e.getMessage());
        }
    }

    @Test
    public void test_doUserManagement(){
        // returns null
        assertNull(samlHandler.doUserManagement(null));
        Assertion assertion1 = Helpers.buildSAMLObject(Assertion.class);
        assertNull(samlHandler.doUserManagement(assertion1));
        Assertion assertion2 = Helpers.buildSAMLObject(Assertion.class);
        assertion2.getAttributeStatements().add(Helpers.buildSAMLObject(AttributeStatement.class));
        assertNull(samlHandler.doUserManagement(assertion2));

        // returns null
        Assertion assertion3 = Helpers.buildSAMLObject(Assertion.class);
        assertion3.setIssueInstant(Instant.ofEpochMilli(0));
        assertion3.setVersion(SAMLVersion.VERSION_20);
        assertion3.setID("ASSERTION_3");
        AttributeStatement anyAttrStmt = Helpers.buildSAMLObject(AttributeStatement.class);
        Attribute anyAttribute = Helpers.buildSAMLObject(Attribute.class);
        anyAttribute.setName("anyKey");
        final XSString value = valueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        value.setValue("bar");
        anyAttribute.getAttributeValues().add(value);
        anyAttrStmt.getAttributes().add(anyAttribute);
        assertion3.getAttributeStatements().add(anyAttrStmt);
    }

    @Test
    public void test_authn_request(){
        AuthnRequest authnRequest = samlHandler.buildAuthnRequest();
        assertNotNull(authnRequest);
        assertEquals(samlHandler.getSaml2IDPDestination(), authnRequest.getDestination());
        assertTrue(authnRequest.getIssueInstant().isBefore(Instant.now()));
        assertEquals(SAMLConstants.SAML2_POST_BINDING_URI, authnRequest.getProtocolBinding());
        assertEquals(samlHandler.getACSURL(), authnRequest.getAssertionConsumerServiceURL());
        assertTrue(authnRequest.getID().length()==33);
    }

    @Test
    public void test_decodeHttpPostSamlResp(){
        SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        // <?xml version="1.0" encoding="UTF-8"?>
        // <samlp:Response ID="foo" IssueInstant="1970-01-01T00:00:00.000Z" Version="2.0"
        //   xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol">
        //   <samlp:Status>
        //      <samlp:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
        //   </samlp:Status>
        // </samlp:Response>
        when(request.getParameter("SAMLResponse")).thenReturn("PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHNhbWxwOlJlc3Bvbn"
            + "NlIElEPSJmb28iIElzc3VlSW5zdGFudD0iMTk3MC0wMS0wMVQwMDowMDowMC4wMDBaIiBWZXJzaW9uPSIyLjAiIHhtbG5zOnN"
            + "hbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiPjxzYW1scDpTdGF0dXM+PHNhbWxwOlN0YXR1c0Nv"
            + "ZGUgVmFsdWU9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpzdGF0dXM6U3VjY2VzcyIvPjwvc2FtbHA6U3RhdHVzPjwvc"
            + "2FtbHA6UmVzcG9uc2U+");
        MessageContext messageContext = samlHandler.decodeHttpPostSamlResp(request);
        assertTrue(messageContext.getMessage() instanceof Response);
        Response response = (Response) messageContext.getMessage();
        assertEquals("urn:oasis:names:tc:SAML:2.0:status:Success", response.getStatus().getStatusCode().getValue());
        assertEquals("foo", response.getID());
        assertEquals("urn:oasis:names:tc:SAML:2.0:protocol", response.getElementQName().getNamespaceURI());
    }

    @Test
    public void test_buildIssuer(){
        Issuer issuer = samlHandler.buildIssuer();
        assertEquals(samlHandler.getEntityID(), issuer.getValue());
    }

    @Test
    public void test_buildNameIdPolicy(){
        NameIDPolicy nameIDPolicy = samlHandler.buildNameIdPolicy();
        assertTrue(nameIDPolicy.getAllowCreate());
        assertEquals("urn:oasis:names:tc:SAML:2.0:nameid-format:transient", nameIDPolicy.getFormat());
    }

    @Test
    public void test_getIPDEndpoint(){
        Endpoint endpoint = samlHandler.getIPDEndpoint();
        assertEquals("http://localhost:8080/idp/profile/SAML2/Redirect/SSO", endpoint.getLocation());
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect",endpoint.getBinding());
    }

    @Test
    public void test_getSLOEndpoint(){
        Endpoint endpoint = samlHandler.getSLOEndpoint();
        assertEquals("https://sling.apache.org/", endpoint.getLocation());
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:PAOS",endpoint.getBinding());
    }

    @Test
    public void test_saml2user(){
        Saml2User samlUser = new Saml2User("test-user");
        Saml2User samlUser2 = new Saml2User();
        samlUser2.setId("test-user");
        assertTrue(samlUser.getId().equals(samlUser2.getId()));
        samlUser.addGroupMembership("test-group");
        XSString xmlObject = Mockito.mock(XSString.class);
        when(xmlObject.getValue()).thenReturn("212-555-1234");
        samlUser.addUserProperty("phone", xmlObject);
        assertEquals("212-555-1234",samlUser.getUserProperties().get("phone"));
        assertEquals("test-user", samlUser.getId());
        assertTrue( samlUser.getGroupMembership().contains("test-group"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void test_buildSAMLObjectNoSuchFieldException(){
        Helpers.buildSAMLObject(Resource.class);
    }

    @Test
    public void test_withJKS() throws NoSuchAlgorithmException, CertificateException, CertIOException, OperatorCreationException, KeyStoreException {
        assertEquals("./target/exampleSaml2.jks", saml2handlerJKS.getJksFileLocation());
        assertEquals("password", saml2handlerJKS.getJksStorePassword());
        assertTrue(saml2handlerJKS.getSaml2SPEncryptAndSign());
        assertTrue(saml2handlerJKS.getSaml2SPEnabled());
    }

    @Test
    public void test_JKS_sp_KeyPair() {
        BasicX509Credential spX509Cred = KeyPairCredentials
            .getCredential( saml2handlerJKS.getJksFileLocation(),
                saml2handlerJKS.getJksStorePassword().toCharArray(),
                saml2handlerJKS.getSpKeysAlias(),
                saml2handlerJKS.getSpKeysPassword().toCharArray()
            );
        Credential idpCert = VerifySignatureCredentials.getCredential(saml2handlerJKS.getJksFileLocation(),
                saml2handlerJKS.getJksStorePassword().toCharArray(),
                saml2handlerJKS.getIdpCertAlias());
        assertEquals(saml2handlerJKS.getIdpVerificationCert().getPublicKey().toString(), idpCert.getPublicKey().toString());
        assertEquals(saml2handlerJKS.getSpKeypair().getPublicKey().toString(), spX509Cred.getPublicKey().toString());
    }

    @Test (expected = SAML2RuntimeException.class)
    public void test_JKS_bad_sp_KeyPair() {
        BasicX509Credential spX509Cred = KeyPairCredentials
            .getCredential( saml2handlerJKS.getJksFileLocation(),
                    saml2handlerJKS.getJksStorePassword().toCharArray(),
                    saml2handlerJKS.getSpKeysAlias(),
                    "bad password".toCharArray()
                );
    }

    @Test (expected = SAML2RuntimeException.class)
    public void test_JKS_bad_idp_cert() {
        Credential idpX509Cred = VerifySignatureCredentials
            .getCredential( saml2handlerJKS.getJksFileLocation(),
                    "bad password".toCharArray(),
                    saml2handlerJKS.getIdpCertAlias()
            );
    }


    void setup_saml2handlerJKS(){
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("jksFileLocation","./target/exampleSaml2.jks");
        props.put("saml2SPEnabled",true);
        props.put("saml2SPEncryptAndSign",true);
        props.put("jksStorePassword","password");
        props.put("idpCertAlias","idpCertAlias");
        props.put("spKeysAlias","spAlias");
        props.put("spKeysPassword","sppassword");
        try {
            saml2handlerJKS = osgiContext.registerInjectActivateService(new AuthenticationHandlerSAML2Impl(), props);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
