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

import org.apache.sling.servlethelpers.internalrequests.SlingInternalRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ApiPlanesIT extends ApiPlanesTestSupport {

    @Inject
    private BundleContext bundleContext;
    
    private static TestServlet fooServlet;

    private static final String TEST_EXTENSION = "defaultTestServlet";

    @Before
    public void setup() {
        if(fooServlet == null) {
            fooServlet = new TestServlet()
                .with(TestServlet.P_RESOURCE_TYPES, TestServlet.RT_DEFAULT)
                .with(TestServlet.P_EXTENSIONS, TEST_EXTENSION)
                .register(bundleContext);
        }
    }

    @Test
    public void defaultPlane() throws Exception {
        fooServlet.assertSelected(new SlingInternalRequest(resourceResolver, requestProcessor, "/")
            .withExtension(TEST_EXTENSION)
        );
    }
}