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
package org.apache.sling.rewriter.it.tests;

import javax.inject.Inject;

import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.resource.presence.ResourcePresence;
import org.apache.sling.testing.paxexam.TestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.service.http.HttpService;

import static org.apache.sling.testing.paxexam.SlingOptions.slingCommonsHtml;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.apache.sling.testing.paxexam.SlingOptions.slingResourcePresence;
import static org.apache.sling.testing.paxexam.SlingOptions.slingScriptingJavascript;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;

public abstract class RewriterTestSupport extends TestSupport {

    @Inject
    protected ServletResolver servletResolver;

    @Inject
    protected HttpService httpService;

    @Inject
    @Filter(value = "(path=/content/rewriter)")
    private ResourcePresence resourcePresence;

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            baseConfiguration(),
            quickstart(),
            // Sling Rewriter
            testBundle("bundle.filename"),
            slingCommonsHtml(),
            factoryConfiguration("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended")
                .put("user.mapping", new String[]{"org.apache.sling.rewriter=sling-readall"})
                .asOption(),
            factoryConfiguration("org.apache.sling.resource.presence.internal.ResourcePresenter")
                .put("path", "/content/rewriter")
                .asOption(),
            // testing
            mavenBundle().groupId("org.jsoup").artifactId("jsoup").versionAsInProject(),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").versionAsInProject(),
            slingResourcePresence(),
            junitBundles()
        };
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(final TestProbeBuilder testProbeBuilder) {
        testProbeBuilder.setHeader("Sling-Initial-Content",
            "apps/esp;path:=/apps/esp;overwrite:=true;uninstall:=true" + "," +
                "content;path:=/content;overwrite:=true;uninstall:=true"
        );
        return testProbeBuilder;
    }

    protected Option quickstart() {
        final int httpPort = findFreePort();
        final String workingDirectory = workingDirectory();
        return composite(
            slingQuickstartOakTar(workingDirectory, httpPort),
            slingScriptingJavascript()
        );
    }

}
