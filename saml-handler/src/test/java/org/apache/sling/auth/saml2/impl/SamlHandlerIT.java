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

package org.apache.sling.auth.saml2.impl;
import org.apache.sling.testing.paxexam.SlingOptions;
import org.apache.sling.testing.paxexam.TestSupport;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import static org.apache.sling.testing.paxexam.SlingOptions.logback;
import static org.apache.sling.testing.paxexam.SlingOptions.slingQuickstartOakTar;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;


public class SamlHandlerIT extends TestSupport {

    static int HTTP_PORT = 8080;
    static int DEST_HTTP_PORT = 8484;

    @Configuration
    public Option[] configuration() {
        // Patch versions of features provided by SlingOptions
        HTTP_PORT = findFreePort();
        DEST_HTTP_PORT = findFreePort();
        SlingOptions.versionResolver.setVersionFromProject("org.apache.sling", "org.apache.sling.auth.saml2");
        return new Option[]{
            systemProperty("org.osgi.service.http.port").value(String.valueOf(HTTP_PORT)),
            baseConfiguration(),
            slingQuickstart(),
            junitBundles(),
            logback(),
            // build artifact
            testBundle("bundle.filename"), // from TestSupport
            CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.webconsole.plugins.ds", "2.0.8")
        };
    }

    protected Option slingQuickstart() {
        final String workingDirectory = workingDirectory(); // from TestSupport
        return composite(
            slingQuickstartOakTar(workingDirectory, 8081) // from SlingOptions
        );
    }


    @Test
    public void exampleTest(){
        System.err.println("test log");
        assertTrue(true);
    }
}
