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
package org.apache.sling.scripting.graphql.it;

import javax.inject.Inject;
import javax.script.ScriptEngineFactory;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.graphql.api.DataFetcherProvider;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.options.ModifiableCompositeOption;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;

import org.apache.sling.engine.SlingRequestProcessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.apache.sling.testing.paxexam.SlingOptions.slingResourcePresence;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScripting;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScriptingJsp;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GraphQLScriptingTestSupport extends TestSupport {

    private final static int STARTUP_WAIT_SECONDS = 30;

    @Inject
    @Filter(value = "(names=graphql)")
    protected ScriptEngineFactory scriptEngineFactory;

    @Inject
    private ResourceResolverFactory resourceResolverFactory;

    @Inject
    protected SlingRequestProcessor requestProcessor;

    protected ServiceRegistration<DataFetcherProvider> dataFetcherFactoryRegistration;

    @Inject
    private BundleContext bundleContext;

    @Before
    public void registerFetchers() {
        PipeDataFetcherFactory pipeDataFetcherFactory = new PipeDataFetcherFactory();
        dataFetcherFactoryRegistration =
                bundleContext.registerService(DataFetcherProvider.class, pipeDataFetcherFactory, null);
    }

    @After
    public void unregisterFetchers() {
        dataFetcherFactoryRegistration.unregister();
    }

    public ModifiableCompositeOption baseConfiguration() {
        final String vmOpt = System.getProperty("pax.vm.options");

        return composite(
            when(vmOpt != null).useOptions(
                vmOption(vmOpt)
            ),
            super.baseConfiguration(),
            slingQuickstart(),
            graphQLJava(),
            testBundle("bundle.filename"),
            newConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist")
                .put("whitelist.bundles.regexp", "^PAXEXAM.*$")
                .asOption(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.servlet-helpers").versionAsInProject(),
            mavenBundle().groupId("com.google.code.gson").artifactId("gson").versionAsInProject(),
            slingResourcePresence(),
            jsonPath(),
            junitBundles()
        );
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(final TestProbeBuilder testProbeBuilder) {
        testProbeBuilder.setHeader("Sling-Initial-Content", "initial-content");
        return testProbeBuilder;
    }

    protected Option slingQuickstart() {
        final int httpPort = findFreePort();
        final String workingDirectory = workingDirectory();
        return composite(
            slingQuickstartOakTar(workingDirectory, httpPort),
            slingScripting(),
            slingScriptingJsp()
        );
    }

    protected Option jsonPath() {
        return composite(
            mavenBundle().groupId("com.jayway.jsonpath").artifactId("json-path").versionAsInProject(),
            mavenBundle().groupId("net.minidev").artifactId("json-smart").versionAsInProject(),
            mavenBundle().groupId("net.minidev").artifactId("accessors-smart").versionAsInProject(),
            mavenBundle().groupId("org.ow2.asm").artifactId("asm").versionAsInProject(),
            mavenBundle().groupId("com.jayway.jsonpath").artifactId("json-path-assert").versionAsInProject(),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").versionAsInProject()
        );
    }

    protected Option graphQLJava() {
        return composite(
            mavenBundle().groupId("com.graphql-java").artifactId("graphql-java").versionAsInProject(),
            mavenBundle().groupId("org.antlr").artifactId("antlr4-runtime").versionAsInProject(),
            mavenBundle().groupId("com.graphql-java").artifactId("java-dataloader").versionAsInProject(),
            mavenBundle().groupId("org.reactivestreams").artifactId("reactive-streams").versionAsInProject()
        );
    }

    /**
     * Injecting the appropriate services to wait for would be more elegant but this is very reliable..
     */
    @Before
    public void waitForSling() throws Exception {
        final int expectedStatus = 200;
        final List<Integer> statuses = new ArrayList<>();
        final String path = "/.json";
        final long endTime = System.currentTimeMillis() + STARTUP_WAIT_SECONDS * 1000;

        while (System.currentTimeMillis() < endTime) {
            final int status = executeRequest("GET", path, null, -1).getStatus();
            statuses.add(status);
            if (status == expectedStatus) {
                return;
            }
            Thread.sleep(250);
        }

        fail("Did not get a " + expectedStatus + " status at " + path + " got " + statuses);
    }

    protected MockSlingHttpServletResponse executeRequest(final String method, 
        final String path, Map<String, Object> params, final int expectedStatus) throws Exception {
        final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
        assertNotNull("Expecting ResourceResolver", resourceResolver);
        final MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(resourceResolver) {
            @Override
            public String getMethod() {
                return method;
            }
        };

        request.setPathInfo(path);

        if(params != null) {
            request.setParameterMap(params);
        }

        final MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        requestProcessor.processRequest(request, response, resourceResolver);

        if (expectedStatus > 0) {
            assertEquals("Expected status " + expectedStatus + " for " + method
                + " at " + path + " - content=" + response.getOutputAsString(), expectedStatus, response.getStatus());
        }

        return response;
    }

    protected String getContent(String path) throws Exception {
        return executeRequest("GET", path, null, 200).getOutputAsString();
    }

    protected String getContent(String path, String ... params) throws Exception {
        return executeRequest("GET", path, toMap(params), 200).getOutputAsString();
    }

    protected Map<String, Object> toMap(String ...keyValuePairs) {
        final Map<String, Object> result = new HashMap<>();
        for(int i=0 ; i < keyValuePairs.length; i+=2) {
            result.put(keyValuePairs[i], keyValuePairs[i+1]);
        }
        return result;
    }
}
