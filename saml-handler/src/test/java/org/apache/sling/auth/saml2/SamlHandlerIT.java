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

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.ModifiableCompositeOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import static org.apache.sling.auth.core.spi.AuthenticationHandler.REQUEST_LOGIN_PARAMETER;
import static org.apache.sling.auth.saml2.impl.JKSHelper.IDP_ALIAS;
import static org.apache.sling.auth.saml2.impl.JKSHelper.SP_ALIAS;
import static org.apache.sling.testing.paxexam.SlingOptions.logback;
import static org.apache.sling.testing.paxexam.SlingOptions.slingAuthForm;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.apache.sling.testing.paxexam.SlingOptions.versionResolver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

/**
 * PAX Exam Integration Tests for AuthenticationHandlerSaml2 and Saml2UserMgtService
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SamlHandlerIT extends TestSupport {

    static int HTTP_PORT = 8080;
    static int DEST_HTTP_PORT = 8484;
    private static Logger logger = LoggerFactory.getLogger(SamlHandlerIT.class);
    ResourceResolver resourceResolver = null;
    Session session;
    JackrabbitSession jrSession;
    UserManager userManager;

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected ConfigurationAdmin configurationAdmin;

    @Inject
    AuthenticationSupport authenticationSupport;

    @Inject
    HttpService httpService;

    @Inject
    ResourceResolverFactory resolverFactory;

    @Inject
    @Filter(value = "(saml2SPEncryptAndSign=false)")
    AuthenticationHandler authHandler;

    @Inject
    @Filter(value = "(saml2SPEncryptAndSign=true)")
    AuthenticationHandler authHandlerEnc;

    @Inject
    Saml2UserMgtService saml2UserMgtService;

    @Configuration
    public Option[] configuration() {
        // Patch versions of features provided by SlingOptions
        HTTP_PORT = findFreePort();
        DEST_HTTP_PORT = findFreePort();
//        String OAK_VERSION = "1.32.0";
        String OAK_VERSION = "1.38.0";
        versionResolver.setVersion("commons-codec", "commons-codec", "1.14");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-jackrabbit-api", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-auth-external", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-api", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-core-spi", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-commons", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "jackrabbit-jcr-commons", "2.20.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-blob-plugins", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-store-spi", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-core", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-blob", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-store-composite", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-store-document", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-jcr", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-lucene", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-authorization-principalbased", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-query-spi", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-security-spi", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-segment-tar", OAK_VERSION);
        SlingOptions.versionResolver.setVersion("org.apache.tika", "tika-core", "1.24");
        SlingOptions.versionResolver.setVersion("org.apache.sling", "org.apache.sling.jcr.oak.server", "1.2.10");
        SlingOptions.versionResolver.setVersion("org.apache.sling", "org.apache.sling.api", "2.23.0");
        SlingOptions.versionResolver.setVersion("org.apache.sling", "org.apache.sling.resourceresolver", "1.7.2");
        SlingOptions.versionResolver.setVersion("org.apache.sling", "org.apache.sling.scripting.core", "2.3.4");
        SlingOptions.versionResolver.setVersion("org.apache.sling", "org.apache.sling.commons.compiler", "2.4.0");
        SlingOptions.versionResolver.setVersion("org.apache.sling", "org.apache.sling.servlets.resolver", "2.7.12");
        Option[] options = new Option[]{
            systemProperty("org.osgi.service.http.port").value(String.valueOf(HTTP_PORT)),
            systemProperty("sling.home").value("./sling"),
            baseConfiguration(),
            slingQuickstart(),
            slingAuthForm(),
            failOnUnresolvedBundles(),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-jackrabbit-api").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-auth-external").version(versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.converter").version("1.0.14"),
            mavenBundle().groupId("org.mockito").artifactId("mockito-core").version("3.3.3"),
            mavenBundle().groupId("net.bytebuddy").artifactId("byte-buddy").version("1.10.5"),
            mavenBundle().groupId("net.bytebuddy").artifactId("byte-buddy-agent").version("1.10.5"),
            mavenBundle().groupId("org.objenesis").artifactId("objenesis").version("2.6"),
            mavenBundle().groupId("org.bouncycastle").artifactId("bcprov-jdk15on").version("1.64"),
            mavenBundle().groupId("org.bouncycastle").artifactId("bcpkix-jdk15on").version("1.64"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.commons.compiler").version(versionResolver),
            factoryConfiguration("org.apache.sling.jcr.repoinit.RepositoryInitializer")
                .put("scripts", new String[]{"create service user saml2-user-mgt\n\n  set ACL for saml2-user-mgt\n\n  allow jcr:all on /home\n\n  end\n\n  create group sling-authors with path /home/groups/sling-authors"})
                .asOption(),
            newConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist")
                .put("whitelist.bypass", "true").asOption(),
            // build artifact
            junitBundles(),
            logback(),
            optionalRemoteDebug(),
            optionalJacoco(),
            factoryConfiguration("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended")
                .put("user.mapping", new String[]{"org.apache.sling.auth.saml2:Saml2UserMgtService=saml2-user-mgt"})
                .asOption(),
            newConfiguration("org.apache.sling.engine.impl.auth.SlingAuthenticator")
                .put("auth.annonymous", false)
                .asOption(),
            // supply the required configuration so the auth handler service will activate
            testBundle("bundle.filename"), // from TestSupport
//                urn:oid:1.2.840.113549.1.9.1=profile/email
//                urn:oid:2.5.4.4=profile/surname
//                urn:oid:2.5.4.42=profile/givenName
//                phone=profile/phone is configured but not included in assertion
            factoryConfiguration("org.apache.sling.auth.saml2.AuthenticationHandlerSAML2")
                .put("path", "/")
                .put("entityID", "http://localhost:8080/")
                .put("acsPath", "/sp/consumer")
                .put("saml2userIDAttr", "urn:oid:0.9.2342.19200300.100.1.1")
                .put("saml2userHome", "/home/users/saml")
                .put("saml2groupMembershipAttr", "urn:oid:2.16.840.1.113719.1.1.4.1.25")
                .put("syncAttrs", new String[]{"urn:oid:2.5.4.4=./profile/surname","urn:oid:2.5.4.42=./profile/givenName","phone=./profile/phone","urn:oid:1.2.840.113549.1.9.1=./profile/email"})
                .put("saml2SPEnabled", true)
                .put("saml2SPEncryptAndSign", false)
                .put("jksFileLocation", "")
                .put("jksStorePassword", "")
                .put("idpCertAlias","")
                .put("spKeysAlias","")
                .put("spKeysPassword","")
                .asOption(),
            factoryConfiguration("org.apache.sling.auth.saml2.AuthenticationHandlerSAML2")
                .put("path", "/")
                .put("entityID", "http://localhost:8080/")
                .put("acsPath", "/sp/consumer")
                .put("saml2userIDAttr", "username")
                .put("saml2userHome", "/home/users/saml")
                .put("saml2groupMembershipAttr", "groupMembership")
                .put("syncAttrs", new String[]{"urn:oid:2.5.4.4","urn:oid:2.5.4.42","phone","urn:oid:1.2.840.113549.1.9.1"})
                .put("saml2SPEnabled", true)
                .put("saml2SPEncryptAndSign", true)
                .put("jksFileLocation", "./src/test/resources/exampleSaml2.jks")
                .put("jksStorePassword", "password")
                .put("idpCertAlias",IDP_ALIAS)
                .put("spKeysAlias",SP_ALIAS)
                .put("spKeysPassword","sppassword")
                .asOption(),
        };
        return options;
    }

    /**
     * Optionally configure remote debugging on the port supplied by the "debugPort"
     * system property.
     */
    protected ModifiableCompositeOption optionalRemoteDebug() {
        VMOption option = null;
        String property = System.getProperty("debugPort");
        if (property != null) {
            option = vmOption(String.format("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%s", property));
        }
        return composite(option);
    }

    protected ModifiableCompositeOption optionalJacoco(){
        VMOption jacocoCommand = null;
        final String jacocoOpt = System.getProperty("jacoco.command");
        if (StringUtils.isNotEmpty(jacocoOpt)) {
            jacocoCommand = new VMOption(jacocoOpt);
        }
        return composite(jacocoCommand);
    }

    protected Option slingQuickstart() {
        final String workingDirectory = workingDirectory(); // from TestSupport
        return composite(
            slingQuickstartOakTar(workingDirectory, HTTP_PORT) // from SlingOptions
        );
    }

    protected Bundle findBundle(final String symbolicName) {
        for (final Bundle bundle : bundleContext.getBundles()) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                return bundle;
            }
        }
        return null;
    }

    void logBundles() {
        for (final Bundle bundle : bundleContext.getBundles()) {
            // logs to target/test.log
            String active = bundle.getState() == Bundle.ACTIVE ? "active" : ""+bundle.getState();
            logger.info(bundle.getSymbolicName()+":"+bundle.getVersion().toString()+ "state:"+active);
        }
    }

    @Before
    public void before(){
        try {
            resourceResolver = resolverFactory.getAdministrativeResourceResolver(null);
            session = resourceResolver.adaptTo(Session.class);
            jrSession = (JackrabbitSession) session;
            userManager = jrSession.getUserManager();
        } catch (RepositoryException | LoginException e) {
            fail(e.getMessage());
        }
        saml2UserMgtService.setUp();
    }

    @After
    public void after(){
        resourceResolver.close();
        saml2UserMgtService.cleanUp();
        session = null;
        jrSession = null;
        userManager = null;
    }

    @Test
    public void test_setup(){
        assertNotNull(bundleContext);
        assertNotNull(configurationAdmin);
        assertNotNull(authenticationSupport);
        assertNotNull(httpService);
        assertNotNull(resolverFactory);
        assertNotNull(saml2UserMgtService);
        assertNotNull(authHandler);
        assertNotNull(authHandlerEnc);
        logBundles();
    }

    @Test
    public void test_samlBundleActive(){
        Bundle samlBundle = findBundle("org.apache.sling.auth.saml2");
        assertEquals(Bundle.ACTIVE, samlBundle.getState());
    }

    @Test
    public void test_userServiceSetup(){
        assertTrue(saml2UserMgtService.setUp());
    }

    @Test
    public void test_getOrCreateSamlUser(){
        saml2UserMgtService.setUp();
        Saml2User saml2User = new Saml2User();
        saml2User.setId("example-saml");
        User user = saml2UserMgtService.getOrCreateSamlUser(saml2User);
        assertNotNull(user);
        assertTrue(saml2UserMgtService.updateUserProperties(saml2User));
        try {
            user.getPath().startsWith("/home/users/saml");
        } catch (RepositoryException e) {
            fail(e.getMessage());
        }
        saml2UserMgtService.cleanUp();
    }

    @Test
    public void test_createSamlUserWithHomePath(){
        saml2UserMgtService.setUp();
        Saml2User saml2User = new Saml2User();
        saml2User.setId("example-saml");
        User user = saml2UserMgtService.getOrCreateSamlUser(saml2User,"/home/users/mypath");
        assertNotNull(user);
        try {
            user.getPath().startsWith("/home/users/mypath");
        } catch (RepositoryException e) {
            fail(e.getMessage());
        }
        saml2UserMgtService.cleanUp();
    }

    @Test
    public void test_groupMembership(){
        saml2UserMgtService.setUp();
        Saml2User saml2User = new Saml2User();
        saml2User.setId("example-saml");
        saml2User.addGroupMembership("sling-authors");
        assertTrue(saml2UserMgtService.updateGroupMembership(saml2User));
        try {
            Authorizable user = userManager.getAuthorizable("example-saml");
            Group group = (Group) userManager.getAuthorizable("sling-authors");
            // confirm that group sling-authors now has a property called managedGroup set to true
            assertTrue(group.isMember(user));
            // confirm that group sling-authors now has a member example-saml
            assertTrue(group.hasProperty("managedGroup"));
            // and is a managed group
            assertTrue(Arrays.stream(group.getProperty("managedGroup")).anyMatch(value -> true));
        } catch (RepositoryException e) {
            fail(e.getMessage());
        }
        saml2UserMgtService.cleanUp();
    }

    @Test
    public void test_request_credentials() throws IOException {
        HttpServletRequest requestIgnore = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        when(requestIgnore.getParameter(REQUEST_LOGIN_PARAMETER)).thenReturn("FORM");
        // request credentials returns false when param ?sling:authRequestLogin=<something other than SAML2>
        assertFalse(authHandler.requestCredentials(requestIgnore,response));
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("/"));
        when(request.getContextPath()).thenReturn("/");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Referer")).thenReturn("/not/my/sp/consumer");
        when(request.getSession()).thenReturn(httpSession);
        // requestCredentials returns true when SAML is enabled and request is not specifying another auth handler
        assertTrue(authHandler.requestCredentials(request,response));
    }

    @Test
    public void test_encACSPath(){
        String base64EndSamlResp = "PHNhbWxwOlJlc3BvbnNlIHhtbG5zOnNhbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiIHhtbG5zOnNhbWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iIERlc3RpbmF0aW9uPSJodHRwOi8vbG9jYWxob3N0OjgwODAvc3AvY29uc3VtZXIiIElEPSJJRF9iOGZhYjUwOC1kOWM3LTQzYmEtOGM4My00Y2I1M2Y3ZjlmZmEiIEluUmVzcG9uc2VUbz0iXzRlNjhiMWIxNTk2ZDE0MmYwZTJhYWMzNjI0YzQxZDFiIiBJc3N1ZUluc3RhbnQ9IjIwMjEtMDQtMDVUMTg6MjE6MTYuMzIxWiIgVmVyc2lvbj0iMi4wIj48c2FtbDpJc3N1ZXI+aHR0cDovL2xvY2FsaG9zdDo4NDg0L2F1dGgvcmVhbG1zL3NsaW5nPC9zYW1sOklzc3Vlcj48c2FtbHA6U3RhdHVzPjxzYW1scDpTdGF0dXNDb2RlIFZhbHVlPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6c3RhdHVzOlN1Y2Nlc3MiLz48L3NhbWxwOlN0YXR1cz48c2FtbDpFbmNyeXB0ZWRBc3NlcnRpb24+PHhlbmM6RW5jcnlwdGVkRGF0YSB4bWxuczp4ZW5jPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzA0L3htbGVuYyMiIFR5cGU9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMDQveG1sZW5jI0VsZW1lbnQiPjx4ZW5jOkVuY3J5cHRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzA0L3htbGVuYyNhZXMxMjgtY2JjIi8+PGRzOktleUluZm8geG1sbnM6ZHM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyMiPjx4ZW5jOkVuY3J5cHRlZEtleT48eGVuYzpFbmNyeXB0aW9uTWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8wNC94bWxlbmMjcnNhLW9hZXAtbWdmMXAiLz48eGVuYzpDaXBoZXJEYXRhPjx4ZW5jOkNpcGhlclZhbHVlPkhHUzU5VDZvMk1RUWtIZ21BYjIyUDBpNlJmRko2YzVBUzhWV2RrUEJIeDhFdHN4R003c0dVUTlLK0VCKzFhaE9NeDJRczZsZjJmc2cwWUo4Q3MvQmJKR056TjFvQ1d6TXc4ZVZmbENtL3dxaGhSS01xKzNDVm5pRHQ1QWoxa1pPZS9XM3RUbEplOE40aFpySVZ0RHBJZ25KdmxzUWM1ckJ3SHZXT2NSanlRd1ZBOFJYYndFYXZDYWFiOEZOWmh1a1BVazUvNEFBMHpTV3N1dUtXYk00d3NrT1BNam5yS3M1R1RhZ0VHQTBuT0tROTBpUGIrSDBPUU1SNUhsamwvRHNtQ0ZWaE1OMlhGc1J2ZHk1aVh0VWk3NStVdXBwMHFYNjRpcjZnSlRyMXJtYnVNQkVVcnJzSGJLN2t0N3JseHdyR0RZMHYrczBndm9ZczZxUDFKcGtHMldXZnNBTmxLQUFGVkdrS2J6eWxrRU1ueDVMNC9GdHJoVjRLUmgvMmV5UjBrL29keElYQmlhQXhPNjNPYXNyczF3ZlFDanN2RFpwYnpEbmhndFdBekVlV0lEZDM1d2o2YisxOEp3N1pTUEFQYzEvVGFVejFodkcvY0hYN05mQnFhbVA0NFFLOHpxQjJkWEdCNTQ3cUVUMk9hWEE1YWc4dW1oTTcvNm5tVEtrVkg1bENabUtWMkQyY1NZUzREK1dlV3FVckZ6NHZoT3Y2WXlIa3gyeTk4VEVMV0VsdDVqaTNTT0g4NmtHWmZvaW1JRUZkaUZwdFdHcWRnd21ZeHZSdGJXK1lXR1YxR01zeVdyclUvMnhLekZ2a29vZ3JGNnVoOGVDbktXM29oc2hGdjdzeERqZFhxakhEQy81d3crUFVFVm1VeDNGbGNzZ0hEUkRyY0tyekw4PTwveGVuYzpDaXBoZXJWYWx1ZT48L3hlbmM6Q2lwaGVyRGF0YT48L3hlbmM6RW5jcnlwdGVkS2V5PjwvZHM6S2V5SW5mbz48eGVuYzpDaXBoZXJEYXRhPjx4ZW5jOkNpcGhlclZhbHVlPmszU0pYVDFXT3lKVFhXOEsyOTFJZW4ySDJWL01MZDd6WlY3aEY5d09FcE1iTmtDL2VETTY4ZFZvQytjMmcwUXpGMFRHYWxWeHJ4Vk1pdmFLbVorNU4rM0VsUk5xc1NxWGpsVCswOXk3RjNXZFUyVXhZdGxVY2VnT1d1bzFCMFUvdFpjVGJGS1FhaHlaQ3IvajBJTVk2NGFsVFo3d1JIREk0eFpDZU1kUTlBVEZXVjdHM1VJbGwxcmhqM3MwZ2l3Ti9kRENEMzBYRy9NTlZ1SGQ5VllMeDB0QUJ5NWdZQm56eFdEemxPL094aUl1OUc0dXdIVjFuU1BESXhtbzlibzM1ZTBya2xscWZRS21TeVZzRWdJQ0xhU05JdllPMjA5ZEwyZDd1RkNoZExraW5IQ2I4UHE1OTZVaFRNUmcxdUZMb1Frb3dnN0toNXgrNTNrVVhKM0xvYjJkL1NkMzMvRUJnUWJGcXFiODM1dk0vMDZCSDNGekFTZG1IRjcxYUtmazRlaW5KaExObWRLOVI0RzQzSGZBUFdJb0NGVGVYU1pSMUwzUEJFOVlnQkZrSzRvZGpFb1ZKaGQ2blJjMXpwdDJjeXdwNHoxRTgza01DZ1hVUDAwWXlibGR2STkrWXdwRXZwZ2NLSkhBdVRNZksyU3p5QnRiNXNjNkFzcllWNkdsdEZyQnZqdzZyVi8yOWhIdFE3Y29iY1NUWUtIWkpDb2pxOWhSSHJUcGVFTXFNTE9yeUpreHJHR3Nodm9HcktWVkhidkNzUmJ3Q2hYRkV6NzU5UWdoeXdGWXJ5Smc0aml6dGg2T21peXphR3Q3VEpRbHNRSkV3YytIOTRHcEJRUWMrWkJSL2Z4dWF4ZWxuOEdVTVhGTmZFbEdvTW1GVG8rSUc2aFdGMWxKZGpzZFdBb056Z2VwRzd3WTBUVUtKV0NQMXVra0JNeitIazJoUWlROHlGTXI2Wll5aUZkVXpyRlFpcVVRY1FKaEd6QWYyUDVvQnNxbkIxN0tpUmc1TVg1UmwvekkrRzM3ZzBCb0t4YStmckJ5OFdlYm51emN4NS9CR3ZwSEltRlJvbS9ORGxTNUEySTBpMWVCTTdsL1lzZFJqTzYwNmlVYlJiUDgxZ2xlZkluQUJIQVRTUWdNN3VyaUlrZzhDNmJIVHVrdVBFSy9NNkx6SmVDYjg5R2ZtYUtnVzRORVJGa0puMVJMY0ZMUEN6VDM4QTRqYjltNFJIYS9iZHVNa1h3NUpwMVR6VGd1VWtHbnNjd1puWUV5WHhHNUdhcTJieWc3aGJLNkY4QVd3YzRGdGVxSk9pVnVvdEswUlFaZUZ6am9jakN2RFRZYU4zdHVZdktvSzM4OFJDWVFVRkx1Y2pKcFdIRGhVV2FEUytxekdEd0tpeUlHTmFvbHFlSXVjbG05UEhoVjhtTHNQZVRRVjlLdklMSFNOMEdsQlpiZk55RXFLZy9wRVFic2Z1amJhTkVON1BIWnF2TktIY2oyZTdtYnZaYjRjYlI4V0xZaENvQTVkdlI0Zks0SkJSYTAxRWJTQmhQb3dPc29LRTIrSDdua0NTVmlvMEdwU0lBQ3NIZUhrS1psU3U1RXdDbm1RdDczbUxhUnlGcVNSQ3JXLzRPUVBRWkY3d1dNaTBjbnVYK25xdTRLcmRBVFVJK2lIQU5DTzI0a3V0UjRWK3ZpT3ozT3lYMksyMkVHaWUwUHlMV2c2ZFlJTXMrQmZrTHJDYTh1dnlvUkZPUDlmajRvc3JjalN1ZXNFTzk5TXBsN0pDMERVQlFNZ1BrUTdjTTFSaENpMGlzMm1hTTVzczdtMUhBZ1EzT1BsN0drbUhSTDY5dEJRM09GbGc3NkxBRzdjbWtieDhKTnVOZVlnRHJTQ2dSVFBKRUlhaStlRVBBNmt1dkRXaDlldTZKdzNqNWt4YjZhK0tQcUNFaEMrQzdlY2N4YW1OUDdEVFFuT3hGQ3hDWjBubWxkdFZrMVFPek1ic29jZ0t4VFMvcTlRMlNUSlI4M1d1WWNyWUZSeWJHeDdrUVRhTERRNmU4eU42bVkxMGZrZDlnMEQ4ZWdWc3g1bUo0UjZkWlNsTWhmUkJYRnR4NnF5c2pvZzZjeEVFeGh6dFhpc1ZZMFQ4YktjNWtzR1F3MTFrVHpJRy9QYlV5Yis0WVgvSGZNYzc0OXllbytJVE1MSE0wRHpvZGsybmg2aEl2dnB0dWpBNmtUSDlwNlJzT1ZJNEZYTnE0RTFZTW56dUJTTFNKNGhHSk8vVThaYjUvWUhhdWszOFVjZGlwTWI2L2crcWhoNVpZMmozamVqVDZXSExPbWVVR1pvb1g3b1plQWpVdkMralYyYU9xRjRmUCtQWjA5cnZSdVpmbHFSM1dtTVpURCs0bUpzSXRQN203OFVod1grL3krTFJvcmlqYjJOK0x4Q1NTOU1BMGhxZFhEeno5NGNOWFdMVURoVzZ4ZlNmMkZ2S0t3UDVFNjJXOUhDbjM4VzhCM2preXkwWFZweitYRlFjZUxQZmtRL0JQaFdkSVU4UEFQbTh6RXIzMC9xRWlQc0hUWkZrczlNTlFsQk94bEJwcVk5RS9nbWt4ZklLbHJKdnhJNzI4NnpxNFFhR0VtRWErbkZHcWlOM1hPS1RING84ZVJhU3pjMk9CZlBWQ2ZmcHZQRUxJY0tSVUM5Q0d2VG1JNEh2V2tkT1NGbGFKME9od1RtWVU1MFp2alFYN2RrUDFTVEhBWVA5UjdSbjl3cGMwNy9INVNrYjI0cFNwSnVsWTRvcGJTaFhEMlJsK2krUTRpWTNIWXNJczZDQVlsNUxMTUpDUWtUbTIzSDUvTFlDZmViUGl3ODBBc3ZKTWpyMDF4dCtoVFNJWEFwUHVsNkFkZzUzZldCSkZuMVErTVlyZ25VRnRHckFxa0hORVJ6b3hpZFVxOHd3STBxejcrazRRMFRLOStJUndyQ1dORE1WdTU1TWpJM2J1T0hoMXJzbEVrZ0xoN0wxMlBGM082NElZblJTWW1HODRkVXZvV0NLY2RFei8wZEtyNFFHTjBUUkV6NUp3eDNYaC9xYTNWOWRkYlBxNFpGbXVzRFl5QzBwNTRaQ0JzcW1qanUzYzNsZWNrK213YlZlRnBzT0FSS0JYU2xzZlpGK3Q3RnhFdzJEdlBnbEY0T2grMFVnd2lLTExJem40MkptR2JvMktLQWx3U1NwVEsvcHdzWlUzRE1XaDVER05DckUxckNuWkdtQXlHRkZiZGMxaUhXNkpocWt1ZjMvN29LSlBaU20zQXZkS0J1ZTlxK1M2QmRIZVh6NlhRNUFyZHBaZk5PQmFVTDQwUVpyODYvZlVUdG5RVlNlWFBNeDVJOVdyT01WYlJtZ096ZFhCdVBxVEtyZ0cvcG1pR0RaK0FhbzRybUFqTUxUQXpyWlY0NmtPMkhRVGZCMFU3aTh4SENLWWx2b1RCUVFZMUtSQlB4VVVyR3pDWXlzc0dFdnpqMUppQVdvZW1PRU05MmJGTkdKRjNaWFBqaS8ycFlQT1pGOHZxejVlWlRUcWdOUDhGcFk0QTJEY01oZGY1bkpDdC9kZXJrcENJcERKT2piN0E1ajBUc3dyWmJHaUN0WXZDQlFpTnk2UUJ3aTZ4UWpIc3hYdGRVeStDV3JWUVAwWm0xSW9KMmFxMXdoYzZMVEhidVNuVHlMUVZSSE5oYzdxek0vdmZoUXluKzY5N3prSzJTV1lvMzJ6ZUsyd3BDQTNLazBWemp6VlVRYkpHQTM1SkJKVlIvQll1THMrV1lxdjM2LzdQZjBiVkgxTmdldlkwVG5zRitvTDJGeTdkZzJNdzNzMmlXeHBNT1pUdEg3R29OVW5VQ0NyM292RXZReldzbmY4dmNJL2w3UERVZVNGN25vMWdHSEtZNzV6WTZrUHhVOTlJbDFEN3pEeXdteHlJdTVHZ1c5c3QxLzFuWEVoSGVXdWtsbTA1RnJKK3ZONnlOcy9iZ3Bkc2s0VnJqbEhFQTdXUTBSZFBMcE1IcXltMnI5cXZySGVBc2VCWktxYnFxVnNIODJnb2ZLc0pTTVJQZkVoSlZ3bjBYYTRNNFVxVFM5U3A1L2tTZWI3WkNWWXhqdU1iYjJ3WGNkZ0VmM0dnVmFobFZRbVZHcGZBNWdGYis2elpsY3pMaWgrTGNLOUxYT3VVNXVUcDViSjBwTkRkZlNVeFpPcG51UUhKS2NIdlV5ZWtHUG82Z0NPYk1aY3V4Z3duR2c0N2F6Tlpqc2xhb0tHTVFld0E3TDBHdjFDbFdSeTB1T3lOVHFzRkU3aW1iL0hJS0hBRklvT3FtRzQ1YUQxMVR4OExyMENGVjR1enJ0WXhyWWNxbkNJZkFlZTNrWnZjSW01SXJSWTdONHA3NXpBVERvYXQ2cWUxZHQ3NFo0OVl4U2trNys2aWxFT0hGbHBETmxwTDR4cVoyVWR4LzNhYkhRZ3Fjb0Rrbld0cU9sZy9LeGpUejdpM2g2OHlVMFhrMVIzUWtpaUNKWFVob3E1aXF0MnFIb2tOZjhuRXRGbjRxeFNTdDZVRWJ5dEI1azA5eGV3UkFsWW5GT3pTdXZ3TWVaUXp2WkFoVDYySVlhN3E4RWplcTRxcGljejBVdVFPMlNJYVkrRG5SaXptTHphTCszVTVSY3lJQkhWTW9qZTh3bXZkZ3l6dHNXQkhta2x2cUhYQ3U4cDR3SXFFYmFxUTY5Um42cWtOWkF6dU1MbE1KV1NEY2hIS0xOb1I0eXRxZlhUNnJCS1R2ZHRObnR3KzN0bi9HL2tKcXVJZ3Bkb1ZxWlN1V1ByVVRMbFpiM3JHaEtSSWp6SFhZbkFZK015cG5wV3Jrai9YendIemtIV2FkQTdQdDRTNDNDQ0RCOWZqejhNWnNBWDJ1YlVmMDRnbXRjRXVoMkFxWXlLVG9VMWgyOTJOeVBwRldnYVhDWm9VQThmNHgrSGVMa1h3Rlh4ZGpiaEVadEZIb1A4Y3JWMzRGTURhcFFkZVJTemR2VjRranVzTjRnWjR6bHFTWWxWVmwzSGxJUDdzTXl6eGNHRUFMa09abXFFQ0hZR29TZHA5eSt6dVN0VW1ZekNMc2pSeGhjSzYrR0FLMENQeXdYZzY5V1hYRHdwUlJnNGpPQkdENDI3S0hqZFU2bU10Vk81NGcxNnNPWWxuOCtoZE44bnhER1UrRi9GNDZIUHFwN01GUnJLZzJXV3VTNVpXNTZGRVIyQUFNb1JsR2gxaW1jY1VsaG9pcmlXeUNRMVJLZ0tXMG91YTJyY3JwaEViUFBkTUU2bjZnbGgvMDJVdmxPbXE1elFzOWZRVVo0R3NQa2M0R01HaWhwZ3JSdkxzV0gxYS9zbWhVenNXSktrZTdWV1ZycUVMRTJsVnpQbTdOZkVDNHJlSlkwUDBpelJ6ZjEzZzg0M3pvcEtKd0Z6WkNLS3MyRjhIcFJKMGVKT0xCdU1mSjUyZVBwYjhSbnpGMTZpVkpkN29nakpvQnZxamo4eEtrbTNsazljRlpGbnQ3TkhIQXRIQlVBWmt2OTJUYXI2cjg5ZUtKS0VTT21CV2RweTJJbUVaT0hmQ1JjRXdBa0RoTXZxeERJTzkrdW8rek1TWTBLcWVQaUdYd3B6eFdzb2JsRW52M0NaR3ZDMGxDaFJ5TXZ3eWhBVElXdEpQZFFkTTY1b2lISzhVK05JNXFNVDRVRmdtcGJMMEFFS29LdkYvOEpraHRzYzB6YmtXeG9WMnpQbndsNWJpSm81N3RpU1RvR2RoZWN2eDdPSVpzOUkxWVo5Q0duL0Jad1p6cnU3ZThHSEgwQlowQTF0b3RxeWdvd2VZL2U3a2hjT1AxQkh1cFhUWkV0aCtmYUVUVERZcXVJOEhMRUsyT0pMcXA4T1MzRnNLN3crbmdTNUVrc2JibVB5aUpXUWxVTVFLRzgxOG5qY3hQSHVIM3gvcHRJWEFqbStyUHJYMFZVNUF1ZDQ3REJTa1JVRCtUa2ZRaTVYdjRGVzJFSlo5VWRTcXBPdS9jMERNT29XOG1uVlJXVGk3QmZMcnplbDc1UU90V1VXcFkxWDU0aDRIenR3RVc4ajhVaTk4RFoyY0ZNaE43bHlXK2NwakZTU1o0d05zcmdKbjNNMkVmUnlSODNGbmhLNGJpcHZRR2hBb1d2eUlTdjNIZEVJckRzeGRBOHlzLzNZY05nMVB1dlpLbnBlN2JrbnkyT2hhZWlOYjZhVHh1NDU4MmhVZlAwZXRuSGRFS29IMXdRcTRKc3J4OXkydFJzejh5d3ROZ1ZvWUVkMzQ2aUVSWXJwVzAyS1cxQ3pWZGdqR2NuRFR1d1Y3MXQ0QmlqNHN5QjNybmVmakx4Q2YvOFRmb0tnQ0N6T05Wb1ErZEVSZEx2VXBvR3dQOEpPaXlleUlWbGcrU1R0RFlyR045eGllUXZldDN0K1NDaUthbkI5MGdlTlhqNXdDRGtwSXV1WDdOamxXNkVRdk1OQkUrUUwzVzBpUzZaWW9UR2tWeDA2OFlQTUN5REdEOUM4d2NoMkRaOERNMGFTQmoxWVoxZSt0QjExRk4xTmFkOXhzVXNLRXEycis3R0Vnb3FxbTBFdWNUUzlsTDA0M1Z4b2F6QktacC8waHFtY3IvNVN0MXdKT0hnYUpzMTBSVWtESTgxdFdLNnFwRVJLR2s1bVZLWTlwd0g3aWpQNEh6WDdSZDk4TzNFclF4bW5uaWwvS01NWHNRemd3d2N0dXVJYUtZbWZ2UnpCWnFSZzZpSnNqT3FZNG5SV3EvOGdVY1d1YzAwdEpPb2Q3SElZang3c29GeXo2enI5M2FDaUxwWnBNTDMyWFNoc2huRFljSm5EUHZUOUZIOG9RcUNwem1hWWNaczdyb01qNXl3Wm5PWlV3Qnh4QjNodUJFUnlzcHgrNDJOWUZTOTAxNitjQjJiRE0rM0ViNWxMRjV3Q2RpWjUzbkdkKytjbDkzNEwwODdCNmlxcWpGU05SbnpMQzVNelErdnVwcVp2cHpzVUlRYlp1UWZrbm5hcTA5Y2ZiVjB6WEI0am5GRWhlYlJyS2hPekxGNUU3NXJzcXZudGpGV0tjYVpQa1VvNUlLSTQ0MElZYlNlQUtFVUc3R01KZStVNzZpSy9zdUYrT2VMaGRqUFV2RXppNzN5NkhoUVZ5QXlrNldyKzcwWVJLcnJidlB4U2tjWEVEb1FaKytmdVpQTFhiRkNBQTl1bjV4TTlZS3pNTFU0UDVyaHgzSTg2dDBTOVB2RUptUnAyRnQ2U1NhdTNqekYxdmhvRjhTSmp1bDNLSlBGZDNkNkQzYmovNVd4c0IrS3J0aDNyU3FUMjlVZFcybi9nUTdMajBQeEJHVFhtc2tlTnIzQVZacGRHRTk4dytsaktCR25McFFKeWxwVkVubDQrOUpEdnRDMTBqNXZoT0ZLN09mVUJuVWM1VXBiNjUzWDcwdFBlaUdvS0dNR0QwZ1Roa1I2L0xqb1RoQUVKVkN6OG1pTzlBcHpiMnZLODR2NHFDY3RxZUFKcS9OZ3FMYlFzMXpKWWFmRjF4V21KSHdzMmZKR2plZnZvQnNFNlhYc005Zk1rK0R0Yk9DWmJhL0NkemxTTXNDUnJJZ0JYdVFON2VTZWtoWEs5T2ZGc09TakFJTXl2YkNZNURvaDh5NTBVZFFDUmVra1dBeG5VeTIwQ3pVSHRnRzdMTkg1dHRrdld2OU9LYkFuWHdSTmhBam5FYVZxU0ovMnYzcEVkUmJEVk5nYy8xcVJnZ1pkY1VIbnJ1eEdQUmRHemNoWmk3OFJYcW13MGtLdFF1c2NrVjRWUFVvbFBvYVp0VkFsbHhUY0lRcHIvQ3kxV3JsdUUrdm9ERlJaTlBMN0N2bGhKcUowT3dEamJYanZHVnYzRW1NMzFGVjZTWU94UGtzYnNwd3ZrVjFTTmsvaURUblNQcmRPT29lT25iNlcraXhHakdOZk5GNmQxYlh1WFdacFdRcjFXZ2pQcE8wZlVoSU5nWDZZNmdZTmhlM29uTG5YQmFhc0hjOWF2RHduQnFlcnl3QTFqQ0RtNzArVWVUN3U0U0xRYUhzRkFONDY4dW1pdkxTSTVjZVE4YXZua1NqSnN3czFkcUF0T1pKWmJsKytzbWdlNWZyTm5teXRQN3AvM25FL05mZXdyWDJha2FLZHFhRC8xSVpUc2Z3NDNaZTMzSklhRGJaSHRmZnBTd25XdWtiQmczdldZeFN1Z200dWhOZkl4M3V2M2xyTUJFYkI3NmVLMUx1eVpsWDdYQ1dMTUxVbTNmS1hwbnIxQUVVUEp5bjdnNU90bUxveWFXNldEOW5hZ3ROZHdhYy9vZHdsRWxpOWJXRTlzcEtOWFdnTWdEZTI2R0w4RXB0NXJUL1Q1cVRpUGkybEorc2Z1SG5vV1RCQkxFK0hHNnVmbnFyeWJKZFpTTkpOQSt3VXB4L1FoUDh3a0RpWWdrc3RLL0pFT2RSU0VyNk5MV3IrMSt5cHRPK1BaV09FMy9UTHBKZkVpaUhnek5vK0Zicmg2WDA2NC8wYWE0ejlQejVCT1oxbGNiVWdWckxBRXZSNENpWGE3Z1RKMmFLTFBLc3FsZ1BubytOcXEvWjZyWUhOOTlPOFoweWNNN2xsUlIySk9iK2dETlBROFdoTlNNeUZFdkJpb1pOQU5UUkdYL2lEMnh1VWM5aGkxTU94UUVxZWNUamZBTVNZUUFxSUJrZjVycktBdFpLeWtOeVVUWndsNTAzNy9WM2lBOG5meVFhWTh6c1d3bDdZakxxSzczODZRK2dTRG5QeUpHY1N2b1JvL1NYN0p2OTJRK0I1eVBLV0tVTUNBWHM1ejRUT01teXFNUzYwSzVXYzZtekhJNDdHd2R6OWlrcmdmaWhlYnV2SVR3bHBBMEtGdWJOMXNHdExMSkpCeUFzQnJ5bjQ5d1JvN2UyV1l1bWozRU5uUzAxVmVVWlVJRFoxYVMvTjRwVGxqN1V2M1ZwLzhnME83anZjanB6bGhwZUlSY3FqMWNsc2E4Sy93YStGNnk5a0RTRlFhSVFaM1JmWXc1a21rbFNHVk1vZWxYekh2MDVsUmh1UVdneVFxK0FYL3J6TDdTRm1Pdmd4eWQrYWhobnhQUjhxdjMyc1owT1ZkS0RiT0Y1Ylg5NFZRRjNCcmhhbHI4TmdTRzZqL21NQnl0cmVUcldGZE1LZkhQdzBtVVdQbWJ4Q2tGOGNrSWV1VFIxbnRmQStFYitDMjZYVW5Hd0RYN1pKSG5oWmV0b0htMTYxWUZMVzJrSGcxN2pzVGllb1kxN2tpRmM1UDBGdFhaeksyQzNabzYydkExZkJhM3RpMjN0Z0t5Mnl5OG1JYWQrWDVoRnJXcWJrMEFSSXh4eU5ZR3V5RE9oMW8vRGhwa05nMnZ0ejdyOTIwRlRmTjUyYTZTWmxPVGtsdlk4WHEwMG1UWUNhdXJxRGZOeVFGY0RBUkk1ME5XVnZiNFZ5cy8rTmZ0dC9sbFNWUUpsd0ZkZEFmaWNsR2xRZi9YdGtXemkrQzlKc0xRSW9kaXZ3NEZkTGk2aVV6MlA2aFR0eEMrd0FOUkZUdGlNcm1Eem12V3laVGJRKzcrVnMzQlZCd0lVSkVlMjdNUW81TUtlci9McGN3ZndyYjhmeEFMbVNjVmxidUlaZXpvWEI0Mkh4T241VVM4TWxXSnU0bjN2RlhJd25TMkdKR0VFdlhMTjdHQ21aOUZDOFdSZE9iUjl6eFZFNElhWXViNisrZ0JHdXRPRjF5cXBmRXU5TVVselJ0RlI4ZklhWDg4MGZRUDlFNHpCVFV5bElwMEV4eGRtWGR4MElUTWJpU0R2MktBOGFPS1FnTkdIalNHRVMwdkh3TmNpem9SdlRvV0tqMGROeWRUdWlvWjUwM3RKMHdSZ1JkaEpKZUoxUEFuQzhMa29xY2ZYRGtSc0R5MTJvQVJXeDFQdklWNFFRZDVia2lQMTRFa2xIZTg2NUlEMVRpVW5kaDFCYmhoUGE0YmVJT2VzdWcyLzdsRmVoM3oyTFVjbFRtczVaMk15NllZblBMczZYelI1WFlranB2TmlqNHZiSEUvb2dlbVUxdnFDSmszbEk2S3J5RW5EQ1h1NjFxRE1UeXhHN3p4MlZzZUNTL0FCcis5OWo2NEZqRzVrQ3RQQWxJbkhFVGJib1l4WWZMVkh2bVptRXRzSkRZTGdYaWdWVUpTUFoyZkJudGZPSjRZWEtQa3dZNk5BaW13dlU0QkwyYmRuOWFUQzhMKytrTDYrYkRBZVd3SzEwR3BWQ0tIRHBINTFjWVBDem4zaHZXMFFGelViejhETldPYTBxcVFlblgzRVZxUWlEdWhWeUVNdHJtbTZOeHFQbWVOLy9KR05ha2taSzlLSysrOVhhdyt5SDg2NFdhNmExVzl4N1A1cmZrUU1wclFtR3BPaExZSHpWT09STXpGRDZUV2poeDZkV25EVnlRVSsvb3lMTndZUk4vKzA0bG1WaFArcHQ2YWE4R3QxTkNRemlxcVRtM2J4VXF4NHU4Z29GRDVWL3paTC9wVFdVY1F4UU5yNUg2aEJHTlNpVGYzQjFCcTMrRjVCeUdKNVpJcGt2dWlGZ3luT0pkejFWTGtjUFNhS0NMOUg4WU00a3dIL0pNRTlxK2ZteTNjRXpaL1hxeTRjU1pUZ3V1YU1lMXptQ2E0Qnk3Sll0OWdRaTl3aVBoNGZPcmhVaDEyQ0VoZm9rUmlxWnQxcVRUbDV0TmpHVFJhLzBKUTBCRXpKamVNd0xBMDkyakZ5NWxZMnYwd0w2WmVkWGFxYXJkdzNRaFlrUmlReS9DRkdadWV3Y0p6TXdXcUFBMmJOdFdHK0hYaGMwS1J4dVY0TDlqVkVjVTNiWDZFS29JVlppeGwwUHpUTytkT3JPbVQyVTdML0dZdXJHZkN3bHErMEVzUFJSODZzU1p5S21VdnpwUC9hZFNvbEdIWE5xbDhMWnlhUldmVktncHRoaWptYjlsZ01qQVRqVGpGQ2VQa1h5M0JYUGhxb3ZjL1lUSXVWM0pXL1NrU1dRSG42K3ZKdmMyOTBVQnNvdnJIeHZlZVUwdDNmMnFMa0NnaXEvQTRhMlhZTmZ2WHZEek5iNHhrY1F3Z2ZZdnhNRTArQ2RtYTFEZWdiTm42Nnd1eWxBNmdXNTVMTXU1cnZMQ2g2WmJhdXVHMCtINFFRVnRjTlppTm15bGxmTDdCMnJzMHZ1ajFGT1o4eXRjbGZNZGZEWnNxenhTbk96dlU3S0hScmdUVHo5M0R1SHE1NWovc1VpTEVkdFhqTUhOMnJBWHBNaktjcTlSU2JhSGhDWjV4TVBYK2c2cmhzTzZSaHA3WXFYMFBiMUorY0hYYytjMVY0RUpXTjM4N0NUbW1FbmJTTnhNZlN2TTFjK1ViVDE1b2lrZElkSnRobitlNThBZVRTbVFtcVBWaDVnVkIwS0ZubFd6cUZHNndwVEROU1pXY0RXMzBQZ21ib3F3cndkcm1aTUlXNjZYNWp1K3FzSEthcUx4WTBwYTZHeDBQSXhUZkZIUmFwa2JwVmpMSVhlT3BWelR3M0NqTDFSNElNc3ZQOXV5ODVFejFldDJBSnY5MGtESnFQb2NKaW9lazdoVmFSV3lFZTRCb3ZBcmRaM1JEUmk4WldXYUtSZmUxY0I0MUtWNGR0K2Y3M05oWE1ITkxpZnQyT2xFSzI4LzRqOUhSVEtpT3hkSFJoV3ozWEhWTUwxNENvOExSVjlLQ3dQdU9FOWQ2NjFVcEo2a241aVc1UWdRcGJ0RUtSQVJLa2ZIUGpWMVhTTmUvM1o4SkFDOFgzaEwvdUVBYXYvcW1BUDdHOGpoREFIY1pTSkFtcjJ3NkRDSnB0Vm5jSGhvNzlZVGUzSFo3VjNTS2ZYMHpXNzlFcDYxSlQrWUV5ZStHR21veks4RWVQR01ybTVPMlBRbjNvRW9IUXZobFc2c2hPRDFid01jMGppdFlXa1ptc3NtdE9Ba2YvaXBMcy9yK1NLYVdNeGdjSlFFTzFzUWJFMGVNQXpEdWJnQmIwM2RZNjh3azJYd3BPRzEwckNDNmN4bHlqOEhYaTMvektQcUs0ckNPQ1o2dVUyU1N0Q2REaVMrLy9YempGWVhrbm1jZFFqTTdkemltMFRLSVpTRHlScC8xa2JyZlRGOTUzc0Q1NjRVb2cxWFR5dnJ1MnFtRjZDYnhnVmZmSFF2RTBtekFYMEZvMStaVkk3eS9NbWxJZnJLaHplU1ZSSkMrWFpHdkVJcXQ4aEZJSXo5bWQ2ZXhtNjVqTVZMaXdXMWdjNTBDK2hiVHRLdkhXU1RRdDZjT0ZhK25KNms1NWRkNTZ4WGdKemh2TGVybmdDTWU5MFBMQzY3Szl5Tkp2UDFUSGVQcnRHWllZKzRjWVMwNmpiMG9ETzR4d0QxYyttalhSK3c3clJzV01MQkZFSUVDV2xOQ0JIdnhnL0VxekNGM3Z2cXdEREhwOS9Mc2txRkxRZkkwd3VaNFkwZkVsVjA4a1MyemFGZkVVOXFtVU9pdnlVZ21OVzR6NjY1NXJNWE9xeDhQWUF5aVU4VnUwQUlJeVFoVTQ3UXpJNzEwUmxOWmQxQjNTRUlET0g1bDBwblM1a3ZzcWRWRDBrVjZONEtYc0ZXc2FnUWZSUmRUQmNpL2d4a0R5amNQWDFSZUNnZHUvdlNSZDI4VStmTzhZOUg1M25CUXdIT3VpZDgyQ1ArZ3RzVWpCcjlWRGtRamloaVlNRE4yK3UwMkdsTlFmQkJ3Ym1LQ2l5VTQ1MkV6blczVlpVWWMyNlpQS3NyK1BnbGwzbHY2Q0R2ZEVpaVFHbnRIV0J1UmNuQTJHUW1GQWZ4SHpLQ011YXRPOC9jR09sWEs0ay8wbWpwVVBlY1RUdkMwcmNKZElNUHdMUllGWmpQbVpjMW5GLzlZQkV3UVozZWp6dlNtNnRCZHpBZmFvWnJUTXlQNlFCaEg2UzBiNjJUem1MWWFDTURHcmFSbThkUy9ZZ3ZyTFlocCtTU3ZBQVlVVlZTR3p5YzNzZFpOOWEyOUUxMGVYYSt4eGd6dlF3ZHdTQnFsSXN6VndUcW1CcURVNkpNT3JnREd0Sk9CTGg5Qmw2YW9Hd3h4UEpKdXRmOUZMR2ZzOVN1cmlXNXhSSlF2bC9lTldvczBhenc1eHgxdkwvbzA0VWhINGoyUzcxSlJyaWF0ajlNaDVUaWNvS1FwOTAraDJ4anhLU2FEcDVLUWdyeHU3Rm1xZEw5VFNKM1dqZWtqT2JTVkJoOTMwMXdIU0E2NDU5aUpOU2NnS2FOUWNyMVdEeWI3Qk9QdnZnZ29IZXJtcnhJeTZraERMRWdpU0twRTA3NnRJZk9OMVBVSis0STErenNDMDlFUEF6OTF5YlpxMkZlS29pVkM5RnBMYVVqcWlBL2xzNjl3SXU2MlpwRkhXMGVvb0RUbVNJYUV6NTlSVTlFOC9Sb3Y0WG9DWjFxWG14emNCcGo2YUpiL0VnYkNJUkREZkp2U2NJcFc1VFRITGdLbXJEckF3SEFNM2dOL0paZjNxYWkwVlF6anBkVjAybHUvZk9aMDRFRFdkVmE4RWd2SUpmODJINXNrKzEvVStMbzhzY2c3Mm10TVBycUVpWnZLNStsRC9FZHRzMlMzL0p4dUQ0UHlid0dYRENRd0dhdytNbzNuYzN4VDNhaTJiRFNvaVgvUFpWMlc5Qjh4N3Roc1IzNzlzVXlBQ2l6c3JZR0tFczB6enZiMzUydDErS0Q2UnE1QnJ4bUpmMkxWTGloa09iZTdqbHpnVE1MRWpWL1RnVzM4TUVyTUtyTldQaytleHVaQUsvNU8yNlZsdTdicmZSU1U0b3NtTVFTZVEzUHBjekZsV1JnaHA0d0hTcXRlTlQ5eXJ6cFZqYm5HUG1xWnZJNVk5RWpBcE9VWkpEZFcveXl3MncyaUl3UGMvbk1ZZU9zdHRKd2RFSDhWYk1NV3B3dklXSkFqYjZ2Q2ZhRUlFa1A0UGVCVWlmNzhCMjdabVdoTVV3ZklRS0x0UnlMZm9MNVFaa1RoblJSczFBYWFiNXVWNDEzNU5hQU52eXBGRjlEMTRTM0d1d3dnQXNMbUR3SzJTU2pXTlMwemFsMDRVODZmTDNHVEhQL2llRFpkbXUrY1A0clNtejhTcVVjNEZUdzd0dGdUbVcrZy80OTNqMWFmSVdxdFBRY3dNZ2U4QzEyM1pCVEEwMGJTN1FDell6UDdRSTFvNk1EOHd5NGZpR0ZLY20vaXVSR3hGNUdNNlBuUU93VTU2TXRIRHp0alhENkFXTVYvYW0wb3dxV3lETWVVcHZGSWVWOCtndVluMFVIWTUvcnZNOWRpdDJtSkxRS0dkYllEWDFFL0E1QzJUUmg0SGZndEM4MzBuK2lXWG1uY2dGTmh1eFBVWlZVYUNxUnZYSy9IczRyVlc5dlpsS1hFK0Q3VmlLK0tQZlVkYXBTTUlpemU0cnZMditGV20xOFQ4RDQ5ZmxrQk1tM0txdW9FbnhpRDg4aml2Z29uYWd1dmdyZWNqUzF6VEU5dWM3ak1ua0tRL3Fyb0NpOHUwQnhJV1ZlVkZna29ZWUxvZGNxN3FQS3g0T29HZ1ZqMW5heC9ZY0dmNEVZZkovK3d5K295YThRWGVzeDh2MjgxYXdnemc4SWJ4a21yaklIaVpXSWZIejlrVk9CK1hsblMyRHhsL254UWNleVpQaVFlQkdvMzVmMWRzSkwvS3RYbnd6ZUh2RWJQSHJQVHVQSG1uVzJnT1ZWV0pKa0RRVEVUR3JrdVJQdi96R1N3T3dKaTZFS0dDbXA2YnhTZ2tPL29McTZLTUdaUXMrMVVFVHB5UGJSYkk3SE9lYzRpamREUHRDVGNEVHhKNVhqYnR1NUlaSTZ0NVp6a25qUnVmclFRTnplMWYzdEVtODdOS0tYT3NQdTVsVFgyVXdidFIraVRjc0ZHaXlLb2VNQ3BZRW5KdHZJTU5MNFc5UzFKL3JsREJsMStZWjBnWXY0Mi8zTk10TDlJODJVcnU4YUxBYTE5Vmp0cG9YUStPWEFFVFVsVVBHMmFRSFNFc1BJbmVYY2NreXd2R3ZURG1GZlBZakNSYzZlWjFlSXRaRjUrTDhZaGZuQjR3MFhMZnFpUFgxc1lJNmJCdVJoUDJST3hHakJDQkFpVXpuN3cvaHh0SkhZS1dseGl5Vnk2V3o5ZWxldndUUU90ZDRhcG1mb3lLU3ZsVGFlNmQ1T3NiQ01GMW8rYTNnQlBqYkUrU2xyemUzclcrTmw2TEtMVmgwTE0wRDU0TFUrWUtNeGNaV1Q2RGJQTTJTNUNlcHk1VHovTEFvZGpTWis5bDF6bkdCZEJrNTdWMXJNZ2NqUDVIZTJ2UkxRV0RoSjhTMzB6OVY0RmRUaVhhN3V4K1lsOEJCaE9INk9GWkpmR25JUS84b2Nvemg0MWwvQU14OHFYaFpwQ0xpeGdGQkNxS0J4d1NFOHdubnZwRlIvZzRDL1BjaTRrUVpIeUN2Z3BuU0wyaEEvMXJBdFdlYXlRdHgxOXlOMWN0QzBqLytBSHdQSUhiV3Z0ZTRaa3A0ejRzNjBqOHJ4S0NDOVY3cEEyaHp2SmxIT2NBTGNZUExPL1ZtYnc4RFp2S25zemVqTkN6ZlVpbzNOK3ZnR3I5NENNTjBzS3dUUHE1WnFVSE1SQ0wwRkhPKzdBMG9LcllQb3ZQaDExczZGY1NSQW5UY0g1QUNkN2pxSC8wMW1YL0x2emp2bWhweXBhWnAvdm9naDIzWC9ubFZQVm1OK1FkaHB5Q3VKb041aTh4MVh1VVJRWmkxckQzYUhXZWVCOEt3ZDRIOVQrWWFTTlJORE0rYStLZzhEU2RrSDJJcnlKU3RkbmU3K1lzQ1ZHaGticzk5WTZMRW1wT2taUExEOFBaWXB0dzBuSC8vR1BpcGkvb3R6UWc5OWJ5VitkaFR2UE1HRHZURUpwV01Wd25QelBqV3FGak9hV2ZLUitFYU9xeUFWbnFHNHJvV04wWW9VTTlML1J1a201ekk1MGd4M3lJSmlLNE41enBFV053RlBHbXNxTU1ZSi95eWZPVHo3MlJCanh3M2M3SE0wQmdOMjl6VDVncURpNDgvSENwSjFsNHhGdzlJTS8xQjFVbGpESS92NmE2bGFWdTFlVEgzLzNqb2VwM21kemFWWHVtb29PZnVDRFZVOUFWT1Vlb2dQdHp0VzdqRlJ4Z29XT1hienBlOUlBU2U2bWpLTGhGZmtpei9CMFJ3NThYUkRoam5XTy9Uck5rWmpOU29jQ01vbG0xVFVTcUM1UDhWK05GN09GSFplVURONUwydzVHbTZnZ20rR05oUXZOSXE3TjZ6QUFoaWtFQ3lZVlJid1lhOXhtUFM0MXIycnIvQzB6eUZmdUliVmtLUXp1WjBvdkU2VW16b1V4bldLbW9EV0Rvc3p2d1BFOTBCT1Z6cnI1MU5wMFhVYzhycG5vZXlhUzdjMzViZy9nZUVFbHNOREEyVmlwOW83aHJtWG1BVEZXb3ZUMUwySXpOczh0a3h3RnlOMGVVVStsM3dzcG45MTYrbG9LYS9EZ1ZTOGZXOU5OSm1pRW9BaTVzNHNRdUpqeWVBZ2NTZGQxL0VQVFl4RWVtQWVJWXFQdU8zRnVpUUlzVGIyVzdpQlN6b2w5QWZXK0kwb0FtRXRYT2xBYmVhdDBJeDBNM080T0dZYjI4aHRLQ3VGTE9pOVNLZGVQSlR5aU03eS9uU2tkVnpJT2ZrVTNqU3RyRysrQWV3ZTBLWm84Z2RHOEQ1R1NSQ3JlYXk0PTwveGVuYzpDaXBoZXJWYWx1ZT48L3hlbmM6Q2lwaGVyRGF0YT48L3hlbmM6RW5jcnlwdGVkRGF0YT48L3NhbWw6RW5jcnlwdGVkQXNzZXJ0aW9uPjwvc2FtbHA6UmVzcG9uc2U+";
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/sp/consumer");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("RelayState")).thenReturn("kojit9j9o1ff9q6vpeo8dnsfc9");
        when(request.getHeader("Origin")).thenReturn("http://localhost:8484");
        when(request.getHeader("Referer")).thenReturn("http://localhost:8484/");
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        when(httpSession.getAttribute("saml2AuthInfo")).thenReturn("kojit9j9o1ff9q6vpeo8dnsfc9");
        when(httpSession.getAttribute("saml2RequestID")).thenReturn("_4e68b1b1596d142f0e2aac3624c41d1b");
        when(request.getSession(false)).thenReturn(httpSession);
        when(request.getParameter("SAMLResponse")).thenReturn(base64EndSamlResp);
        assertNull(authHandlerEnc.extractCredentials(request, response));
        ((AuthenticationFeedbackHandler)authHandler).authenticationFailed(request,response,null);
    }

    @Test
    public void test_goodLogin() throws IOException, RepositoryException {
        userManager.createGroup("all_tenants");
        userManager.createGroup("pcms-authors");
        session.save();
        String base64EndSamlResp = buildAuthResponse();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/sp/consumer");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("RelayState")).thenReturn("ncu7lhndv8o4o096im9065ijqn");
        when(request.getHeader("Origin")).thenReturn("http://localhost:8484");
        when(request.getHeader("Referer")).thenReturn("http://localhost:8484/");
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        when(httpSession.getAttribute("saml2AuthInfo")).thenReturn("ncu7lhndv8o4o096im9065ijqn");
        when(httpSession.getAttribute("saml2RequestID")).thenReturn("_f96afa62dc16b1cc99efab06db0c750d");
        when(request.getSession(false)).thenReturn(httpSession);
        when(request.getSession()).thenReturn(httpSession);
        when(request.getParameter("SAMLResponse")).thenReturn(base64EndSamlResp);
        AuthenticationInfo authenticationInfo = authHandler.extractCredentials(request, response);
        assertNotNull(authenticationInfo);
        assertTrue(((AuthenticationFeedbackHandler)authHandler).authenticationSucceeded(request,response,authenticationInfo));
        authHandler.dropCredentials(request,response);
        User user = (User) userManager.getAuthorizable("saml2Example");
        assertNotNull(user);
        // verify user properties sync
        assertEquals("saml2@example.com", user.getProperty("./profile/email")[0].getString());
        assertEquals("Saml2", user.getProperty("./profile/surname")[0].getString());
        assertEquals("Example", user.getProperty("./profile/givenName")[0].getString());
        // verify group membership
        List groups = new ArrayList<String>();
        groups.add("all_tenants");
        groups.add("authors");
        groups.add("pcms-authors");
        Iterator<Group> groupsIt = user.declaredMemberOf();
        while (groupsIt.hasNext()){
            Group group = groupsIt.next();
            assertTrue(groups.contains(group.getID()));
            // verify managedGroup flag
            assertTrue(group.getProperty("managedGroup")[0].getBoolean());
        }
        // authors group was not created initially and still does not exist
        assertNull(userManager.getAuthorizable("authors"));
    }

    String buildAuthResponse(){
        LocalDateTime dateTime = LocalDateTime.now();
        LocalDateTime notBefore = LocalDateTime.now().minusSeconds(3);
        LocalDateTime notOnOrAfter = LocalDateTime.now().plusMinutes(5);
        String currentTime = dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
        String notOnOrAfterTime = notOnOrAfter.format(DateTimeFormatter.ISO_DATE_TIME);
        String notBeforeTime = notBefore.format(DateTimeFormatter.ISO_DATE_TIME);
        String samlResp0 = "<samlp:Response Destination=\"http://localhost:8080/sp/consumer\" ID=\"ID_5232eed3-8fd5-4562-92bb-69af0246c341\" InResponseTo=\"_f96afa62dc16b1cc99efab06db0c750d\" ";
        String issueInstance = "IssueInstant=\""+currentTime+"\" ";
        String samlResp1 ="Version=\"2.0\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"><saml:Issuer>http://localhost:8484/auth/realms/sling</saml:Issuer><samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></samlp:Status><saml:Assertion ID=\"ID_c78146f1-6c37-4ad5-b2ee-0371667c3aeb\" ";
        //issueInstance
        String samlResp2 = "Version=\"2.0\" xmlns=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml:Issuer>http://localhost:8484/auth/realms/sling</saml:Issuer><saml:Subject><saml:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">G-212b1981-a621-4c67-84ac-cd75551a0250</saml:NameID><saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\"><saml:SubjectConfirmationData InResponseTo=\"_f96afa62dc16b1cc99efab06db0c750d\" ";
        String samlResp3 = "Recipient=\"http://localhost:8080/sp/consumer\"/></saml:SubjectConfirmation></saml:Subject><saml:Conditions ";
        String notBeforeStr = "NotBefore=\""+notBeforeTime+"\" ";
        String notOnOrAfterStr = "NotOnOrAfter=\""+notOnOrAfterTime+"\" ";
        String samlResp4 = "><saml:AudienceRestriction><saml:Audience>http://localhost:8080/</saml:Audience></saml:AudienceRestriction></saml:Conditions><saml:AuthnStatement ";
        String authInstance = "AuthnInstant=\""+notBeforeTime+"\" ";
        String session = "SessionIndex=\"4d647e71-cc20-4779-ad58-7df15816a8c5::901bf7ca-3984-4a5b-8f8b-c1c737738102\" ";
        String sessionNotOnOrAfter = "SessionNotOnOrAfter=\""+notOnOrAfterTime+"\"> ";
        String samlResp5 = "<saml:AuthnContext><saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified</saml:AuthnContextClassRef></saml:AuthnContext></saml:AuthnStatement><saml:AttributeStatement><saml:Attribute FriendlyName=\"givenName\" Name=\"urn:oid:2.5.4.42\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"> <saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">Example</saml:AttributeValue></saml:Attribute><saml:Attribute FriendlyName=\"lastName\" Name=\"lastName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">Saml2</saml:AttributeValue></saml:Attribute><saml:Attribute FriendlyName=\"groupMembership\" Name=\"urn:oid:2.16.840.1.113719.1.1.4.1.25\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">all_tenants</saml:AttributeValue><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">authors</saml:AttributeValue><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">pcms-authors</saml:AttributeValue></saml:Attribute><saml:Attribute FriendlyName=\"email\" Name=\"urn:oid:1.2.840.113549.1.9.1\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">saml2@example.com</saml:AttributeValue></saml:Attribute><saml:Attribute FriendlyName=\"userid\" Name=\"urn:oid:0.9.2342.19200300.100.1.1\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">saml2Example</saml:AttributeValue></saml:Attribute><saml:Attribute FriendlyName=\"surname\" Name=\"urn:oid:2.5.4.4\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">Saml2</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"Role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">uma_authorization</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"Role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">offline_access</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"Role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">view-profile</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"Role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"><saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">manage-account-links</saml:AttributeValue></saml:Attribute><saml:Attribute Name=\"Role\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"> <saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">manage-account</saml:AttributeValue></saml:Attribute></saml:AttributeStatement></saml:Assertion></samlp:Response>";
        String preEncoding =
                samlResp0 +
                issueInstance +
                samlResp1 +
                issueInstance +
                samlResp2 +
                notOnOrAfterStr +
                samlResp3 +
                notBeforeStr +
                notOnOrAfterStr +
                samlResp4 +
                authInstance +
                session +
                sessionNotOnOrAfter +
                samlResp5;
        return Base64.getEncoder().encodeToString(preEncoding.getBytes());
    }
}
