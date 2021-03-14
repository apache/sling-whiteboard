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
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.saml2.impl.AuthenticationHandlerSAML2Impl;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.ModifiableCompositeOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

import static org.apache.sling.testing.paxexam.SlingOptions.logback;
import static org.apache.sling.testing.paxexam.SlingOptions.slingAuthForm;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.apache.sling.testing.paxexam.SlingOptions.versionResolver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
    private final static int STARTUP_WAIT_SECONDS = 30;
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

    @Filter(value = "(authtype=SAML2)")
    @Inject
    AuthenticationHandler authHandler;

    @Inject
    Saml2UserMgtService saml2UserMgtService;


    @Configuration
    public Option[] configuration() {
        // Patch versions of features provided by SlingOptions
        HTTP_PORT = findFreePort();
        DEST_HTTP_PORT = findFreePort();
        versionResolver.setVersion("commons-codec", "commons-codec", "1.14");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-jackrabbit-api", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-auth-external", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-api", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-core-spi", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-commons", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "jackrabbit-jcr-commons", "2.20.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-blob-plugins", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-store-spi", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-core", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-blob", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-store-composite", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-store-document", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-jcr", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-lucene", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-authorization-principalbased", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-query-spi", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-security-spi", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.jackrabbit", "oak-segment-tar", "1.32.0");
        SlingOptions.versionResolver.setVersion("org.apache.tika", "tika-core", "1.24");
        SlingOptions.versionResolver.setVersion("org.apache.sling", "org.apache.sling.jcr.oak.server", "1.2.10");

        return new Option[]{
            systemProperty("org.osgi.service.http.port").value(String.valueOf(HTTP_PORT)),
            baseConfiguration(),
            slingQuickstart(),
            slingAuthForm(),
            failOnUnresolvedBundles(),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-jackrabbit-api").version(versionResolver),
            mavenBundle().groupId("org.apache.jackrabbit").artifactId("oak-auth-external").version(versionResolver),
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
            factoryConfiguration("org.apache.felix.jaas.Configuration.factory")
                .put("jaas.classname", "org.apache.sling.auth.saml2.sp.Saml2LoginModule")
                .put("jaas.controlFlag", "Sufficient")
                .put("jaas.realmName", "jackrabbit.oak")
                .put("jaas.ranking", 110)
                .asOption(),
            newConfiguration("org.apache.sling.engine.impl.auth.SlingAuthenticator")
                .put("auth.annonymous", false)
                .asOption(),
            // supply the required configuration so the auth handler service will activate
            testBundle("bundle.filename"), // from TestSupport
            factoryConfiguration("org.apache.sling.auth.saml2.AuthenticationHandlerSAML2")
                .put("path", "/")
                .put("entityID", "http://localhost:8080/")
                .put("acsPath", "/sp/consumer")
                .put("saml2userIDAttr", "username")
                .put("saml2userHome", "/home/users/saml")
                .put("saml2groupMembershipAttr", "groupMembership")
                .put("syncAttrs", new String[]{"urn:oid:2.5.4.4","urn:oid:2.5.4.42","phone","urn:oid:1.2.840.113549.1.9.1"})
                .put("saml2SPEnabled", true)
                .put("saml2SPEncryptAndSign", false)
                .put("jksFileLocation", "")
                .put("jksStorePassword", "")
                .put("idpCertAlias","")
                .put("spKeysPassword","")
                .asOption(),
        };
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
        logBundles();
    }

    @Test
    public void test_samlBundleActive(){
        Bundle samlBundle = findBundle("org.apache.sling.auth.saml2");
        assertTrue(samlBundle.getState() == Bundle.ACTIVE);
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
}
