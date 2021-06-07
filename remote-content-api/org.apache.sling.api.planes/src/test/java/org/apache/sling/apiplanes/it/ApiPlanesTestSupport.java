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
package org.apache.sling.apiplanes.it;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.servlethelpers.internalrequests.SlingInternalRequest;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScripting;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class ApiPlanesTestSupport extends TestSupport {

    private final static int STARTUP_WAIT_SECONDS = 30;

    @Inject
    private ResourceResolverFactory resourceResolverFactory;
    
    @Inject
    protected SlingRequestProcessor requestProcessor;
    
    protected ResourceResolver resourceResolver;
    
    @Configuration
    public Option[] configuration() {
        final String vmOpt = System.getProperty("pax.vm.options");
        final int httpPort = findFreePort();
        final String workingDirectory = workingDirectory();

        return options(
            composite(
                when(vmOpt != null).useOptions(
                    vmOption(vmOpt)
                ),
                testBundle("bundle.filename"),
                baseConfiguration(),
                slingQuickstartOakTar(workingDirectory, httpPort),
                slingScripting(),
                junitBundles(),
                mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.servlet-helpers").versionAsInProject(),
                newConfiguration("org.apache.sling.jcr.base.internal.LoginAdminWhitelist")
                    .put("whitelist.bundles.regexp", "^PAXEXAM.*$")
                    .asOption()
            )
        );
    }

    @After
    public void cleanup() {
        if(resourceResolver != null) {
            resourceResolver.close();
        }
    }
    
    /**
     * Injecting the appropriate services to wait for would be more elegant but this is very reliable..
     */
    @Before
    public void waitForSling() throws Exception {
        resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

        final int expectedStatus = 200;
        final List<Integer> statuses = new ArrayList<>();
        final String path = "/.json";
        final Instant endTime = Instant.now().plus(Duration.ofSeconds(STARTUP_WAIT_SECONDS));

        while(Instant.now().isBefore(endTime)) {
            final int status = new SlingInternalRequest(resourceResolver, requestProcessor, "/")
                .withExtension("json")
                .execute()
                .checkStatus() // accept any status
                .getResponse().getStatus()
            ;
            
            statuses.add(status);
            if (status == expectedStatus) {
                return;
            }
            Thread.sleep(250);
        }

        fail("Did not get a " + expectedStatus + " status at " + path + " got " + statuses);
    }

}
