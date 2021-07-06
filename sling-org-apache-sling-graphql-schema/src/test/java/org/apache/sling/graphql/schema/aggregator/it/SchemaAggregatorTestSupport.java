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
package org.apache.sling.graphql.schema.aggregator.it;

import java.io.Reader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.servlethelpers.internalrequests.SlingInternalRequest;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.ModifiableCompositeOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScripting;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScriptingJsp;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

public abstract class SchemaAggregatorTestSupport extends TestSupport {

    private final Logger log = LoggerFactory.getLogger(getClass().getName());
    private final static int STARTUP_WAIT_SECONDS = 30;

    @Inject
    protected ResourceResolverFactory resourceResolverFactory;

    @Inject
    protected SlingRequestProcessor requestProcessor;

    protected ModifiableCompositeOption baseConfiguration() {
        final String vmOpt = System.getProperty("pax.vm.options");
        VMOption vmOption = null;
        if (StringUtils.isNotEmpty(vmOpt)) {
            vmOption = new VMOption(vmOpt);
        }

        final String jacocoOpt = System.getProperty("jacoco.command");
        VMOption jacocoCommand = null;
        if (StringUtils.isNotEmpty(jacocoOpt)) {
            jacocoCommand = new VMOption(jacocoOpt);
        }

        return composite(
            when(vmOption != null).useOptions(vmOption),
            when(jacocoCommand != null).useOptions(jacocoCommand),
            super.baseConfiguration(),
            slingQuickstart(),
            testBundle("bundle.filename"),
            newConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist")
                .put("whitelist.bundles.regexp", "^PAXEXAM.*$")
                .asOption(),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.servlet-helpers").versionAsInProject(),
            junitBundles()
        );
    }

    private Option slingQuickstart() {
        final int httpPort = findFreePort();
        log.info("Using HTTP port {}", httpPort);
        final String workingDirectory = workingDirectory();
        return composite(
            slingQuickstartOakTar(workingDirectory, httpPort),
            slingScripting(),
            slingScriptingJsp()
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
        final Instant endTime = Instant.now().plus(Duration.ofSeconds(STARTUP_WAIT_SECONDS));

        while(Instant.now().isBefore(endTime)) {
            final int status = executeRequest("GET", path, null, null, null, -1).getStatus();
            statuses.add(status);
            if (status == expectedStatus) {
                return;
            }
            Thread.sleep(250);
        }

        fail("Did not get a " + expectedStatus + " status at " + path + " got " + statuses);
    }

    protected MockSlingHttpServletResponse executeRequest(final String method, 
        final String path, Map<String, Object> params, String contentType, 
        Reader body, final int expectedStatus) throws Exception {

        // Admin resolver is fine for testing    
        @SuppressWarnings("deprecation")            
        final ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

        final int [] statusParam = expectedStatus == -1 ? null : new int[] { expectedStatus };

        return (MockSlingHttpServletResponse)
            new SlingInternalRequest(resourceResolver, requestProcessor, path)
            .withRequestMethod(method)
            .withParameters(params)
            .withContentType(contentType)
            .withBody(body)
            .execute()
            .checkStatus(statusParam)
            .getResponse()
            ;
    }

    protected String getContent(String path) throws Exception {
        return executeRequest("GET", path, null, null, null, 200).getOutputAsString();
    }
}