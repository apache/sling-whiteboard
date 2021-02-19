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

import org.hamcrest.core.StringStartsWith;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.junit.Test;
import org.hamcrest.Description;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.jmock.api.Action;
import java.io.File;

import static org.apache.sling.auth.saml2.impl.AuthenticationHandlerSAML2Impl.TOKEN_FILENAME;
import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AuthenticationHandlerSAML2Impl.class)
@PowerMockIgnore("jdk.internal.reflect.*")
public class AuthenticationHandlerSAML2ImplTest {

    @Test
    public void test_getTokenFile() {
        final File root = new File("bundle999").getAbsoluteFile();
        final File tokenFileExpected = new File("bundle999/"+TOKEN_FILENAME).getAbsoluteFile();
        final SlingHomeAction slingHome = new SlingHomeAction();
        slingHome.setSlingHome(new File("sling").getAbsolutePath());

        Mockery context = new Mockery();
        final BundleContext bundleContext = context.mock(BundleContext.class);

        context.checking(new Expectations() {
            {
                // mock access to sling.home framework property
                allowing(bundleContext).getProperty("sling.home");
                will(slingHome);

                // mock no data file support with file names starting with sl
                allowing(bundleContext).getDataFile(
                        with(new StringStartsWith("sl")));
                will(returnValue(null));

                // mock data file support for any other name
                allowing(bundleContext).getDataFile(with(any(String.class)));
                will(new RVA(root));
            }
        });
        final AuthenticationHandlerSAML2Impl handler = new AuthenticationHandlerSAML2Impl();
        // test files relative to bundle context
        File tokenFileActual = handler.getTokenFile(bundleContext);
        assertEquals(tokenFileExpected, tokenFileActual);
    }

    @Test
    public void test_getUserid() {
        final AuthenticationHandlerSAML2Impl handler = new AuthenticationHandlerSAML2Impl();
        assertEquals(null, handler.getUserId(null));
        assertEquals(null, handler.getUserId(""));
        assertEquals(null, handler.getUserId("field0"));
        assertEquals(null, handler.getUserId("field0@field1"));
        assertEquals("field3", handler.getUserId("field0@field1@field3"));
        assertEquals("field3@field4", handler.getUserId("field0@field1@field3@field4"));
    }

    /**
     * The <code>SlingHomeAction</code> action returns the current value of the
     * <code>slingHome</code> field on all invocations
     */
    private static class SlingHomeAction implements Action {
        private String slingHome;

        public void setSlingHome(String slingHome) {
            this.slingHome = slingHome;
        }

        public String getSlingHome() {
            return slingHome;
        }

        public Object invoke(Invocation invocation) throws Throwable {
            return slingHome;
        }

        public void describeTo(Description description) {
            description.appendText("returns " + slingHome);
        }
    }

    /**
     * The <code>RVA</code> action returns a file relative to some root file as
     * requested by the first parameter of the invocation, expecting the first
     * parameter to be a string.
     */
    private static class RVA implements Action {

        private final File root;

        RVA(final File root) {
            this.root = root;
        }

        public Object invoke(Invocation invocation) throws Throwable {
            String data = (String) invocation.getParameter(0);
            if (data.startsWith("/")) {
                data = data.substring(1);
            }
            return new File(root, data);
        }

        public void describeTo(Description description) {
            description.appendText("returns new File(root, arg0)");
        }
    }
}

