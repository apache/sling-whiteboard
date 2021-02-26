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
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.saml2.Helpers;
import org.apache.sling.auth.saml2.Saml2UserMgtService;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.opensaml.core.config.InitializationException;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.apache.sling.auth.saml2.Activator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Dictionary;
import java.util.Hashtable;

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
    Saml2UserMgtService userMgtService;
    AuthenticationHandlerSAML2Impl samlHandler;

    @BeforeClass
    public static void initializeOpenSAML(){
        try {
            initializeOpenSaml();
        } catch (InitializationException e) {
            fail(e.getMessage());
        }
    }

    @Before
    public void setup(){
        try {
//            configureAnonAccess();
//            configureJaas();
//            configureUserConfigMgr();
            ResourceResolverFactory mockFactory = Mockito.mock(ResourceResolverFactory.class);
            osgiContext.registerService(ResourceResolverFactory.class, mockFactory);
            userMgtService = osgiContext.registerService(new Saml2UserMgtServiceImpl());
            samlHandler = osgiContext.registerInjectActivateService(new AuthenticationHandlerSAML2Impl());
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

    private void configureJaas() throws IOException {
        final ConfigurationAdmin configAdmin = osgiContext.getService(ConfigurationAdmin.class);
        Configuration jaasConfig = configAdmin.getConfiguration("org.apache.felix.jaas.Configuration.factory");
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("jaas.classname", "org.apache.sling.auth.saml2.sp.Saml2LoginModule");
        props.put("jaas.controlFlag", "Sufficient");
        props.put("jaas.realmName", "jackrabbit.oak");
        props.put("jaas.ranking", 110);
        jaasConfig.update(props);
    }

    private void configureAnonAccess() throws IOException {
        final ConfigurationAdmin configAdmin = osgiContext.getService(ConfigurationAdmin.class);
        Configuration anonConfig = configAdmin.getConfiguration("org.apache.sling.engine.impl.auth.SlingAuthenticator");
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("auth.annonymous", false);
        anonConfig.update(props);
    }

    private void configureUserConfigMgr() throws IOException {
        final ConfigurationAdmin configAdmin = osgiContext.getService(ConfigurationAdmin.class);
        //repoinit
        Configuration repoinitConfig = configAdmin.getConfiguration("org.apache.sling.jcr.repoinit.RepositoryInitializer");
        Dictionary<String, Object> jaasProps = new Hashtable<>();
        jaasProps.put("scripts", new String[]{
"create service user saml2-user-mgt\n\nset ACL for saml2-user-mgt\n\nallow jcr:all on /home\n\nend\n\ncreate group sling-authors with path /home/groups/sling-authors"
        });
        repoinitConfig.update(jaasProps);
        //Service User
        Configuration serviceUserConfig = configAdmin.getConfiguration("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended");
        Dictionary<String, Object> serviceUserProps = new Hashtable<>();
        serviceUserProps.put("user.mapping",new String[]{"org.apache.sling.auth.saml2:Saml2UserMgtService=saml2-user-mgt"});
    }
}
