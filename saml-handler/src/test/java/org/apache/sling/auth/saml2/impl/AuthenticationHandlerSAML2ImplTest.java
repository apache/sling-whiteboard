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

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.hamcrest.core.StringStartsWith;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.junit.Before;
import org.junit.Test;
import org.hamcrest.Description;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.jmock.api.Action;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import static org.apache.sling.auth.saml2.impl.AuthenticationHandlerSAML2Impl.TOKEN_FILENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class AuthenticationHandlerSAML2ImplTest {

    private TokenStore store;
    private static final long sessionTimeoutMsec = 60 * 1000L;
    private static final long defaultExpirationTimeMsec = System.currentTimeMillis() + sessionTimeoutMsec / 2;
    private static final boolean defaultFastSeed = false;
    private static final String userId = "user_" + UUID.randomUUID();
    private String encodedToken;
    private File tokenFile;
    private int additionalFileIndex;

    private File additionalTokenFile() {
        return new File(tokenFile.getParent(), tokenFile.getName() + "-" + additionalFileIndex++);
    }

    @Before
    public void setup() throws Exception {
        tokenFile = File.createTempFile(getClass().getName(), "tokenstore");
        store = new TokenStore(tokenFile, sessionTimeoutMsec, defaultFastSeed);
        encodedToken = store.encode(defaultExpirationTimeMsec, userId);
    }

    @Test
    public void validTokenTest() throws Exception {
        assertTrue(store.isValid(encodedToken));
    }

    @Test
    public void invalidTokensTest() throws Exception {
        final String [] invalid = {
                "1@21@3",
                "nothing",
                "0@bad@token"
        };
        for(String token : invalid) {
            assertFalse(store.isValid(token));
        }
    }

    @Test
    public void expiredTokenTest() throws Exception {
        final String expired = store.encode(1, userId);
        Thread.sleep(50);
        assertFalse(store.isValid(expired));
    }

    @Test
    public void loadTokenFileTest() throws Exception {
        final TokenStore newStore = new TokenStore(tokenFile, sessionTimeoutMsec, defaultFastSeed);
        assertTrue(newStore.isValid(encodedToken));

        final TokenStore emptyStore = new TokenStore(additionalTokenFile(), sessionTimeoutMsec, defaultFastSeed);
        assertFalse(emptyStore.isValid(encodedToken));
    }

    @Test
    public void encodingPartsTest() throws Exception {

        // Test with both a normal and "fast seed" store
        final TokenStore [] testStores = {
                new TokenStore(additionalTokenFile(), sessionTimeoutMsec, true),
                new TokenStore(additionalTokenFile(), sessionTimeoutMsec, false)
        };

        for(TokenStore testStore : testStores) {
            String lastHexNumber = "";
            for(int i=1 ; i < 100; i++) {
                final String uniqueUserId = "user-" + i;
                final String [] parts = TokenStore.split(testStore.encode(123, uniqueUserId));

                // First a unique large hex number
                assertFalse(parts[0].equals(lastHexNumber));
                lastHexNumber = parts[0];
                new BigInteger(lastHexNumber, 16);
                assertTrue(lastHexNumber.length() > 20);

                // Then the timeout prefixed by something else
                assertEquals("123", parts[1].substring(1));

                // Then the user id
                assertEquals(uniqueUserId, parts[2]);
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void nullTokenFileTest() throws Exception {
        new TokenStore(null, sessionTimeoutMsec, defaultFastSeed);
    }

    @Test
    public void test_tokens() throws RepositoryException, UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        // setup handler
        final AuthenticationHandlerSAML2Impl handler = new AuthenticationHandlerSAML2Impl();
        // setup token file
        final File root = new File("target").getAbsoluteFile();
        final File tokenFileExpected = new File("target/"+TOKEN_FILENAME).getAbsoluteFile();
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
        File tokenFileActual = handler.getTokenFile(bundleContext);
        // setup token store
        handler.initializeTokenStore(tokenFileActual);
        // verify token file
        assertEquals(tokenFileExpected, tokenFileActual);

        // setup mock users
        User user = Mockito.mock(User.class);
        when(user.getID()).thenReturn("test-user");
        AuthenticationInfo authenticationInfo = handler.buildAuthInfo(user);
        long not_expired = System.currentTimeMillis() + handler.sessionTimeout;
        String token = handler.getTokenStore().encode(not_expired , authenticationInfo.getUser());
        String[] parts = TokenStore.split(token);
        assertEquals(3, parts.length);
        assertFalse(handler.needsRefresh(token));
        assertTrue(handler.getTokenStore().isValid(token));


        // expired token user
        User userExp = Mockito.mock(User.class);
        when(userExp.getID()).thenReturn("expired-user");
        AuthenticationInfo authenticationInfoExp = handler.buildAuthInfo(userExp);
        long expired = System.currentTimeMillis() - handler.sessionTimeout ;
        String expired_token = handler.getTokenStore().encode(expired, authenticationInfoExp.getUser());
        String[] partsExp = TokenStore.split(expired_token);
        assertEquals(3, partsExp.length);
        assertTrue(handler.needsRefresh(expired_token));
        assertFalse(handler.getTokenStore().isValid(expired_token));

        // test refreshAuthData
        SlingHttpServletRequest request = Mockito.mock(SlingHttpServletRequest.class);
        SlingHttpServletResponse response = Mockito.mock(SlingHttpServletResponse.class);
        HttpSession session = Mockito.mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        handler.getStorageAuthInfo().getString(request);
        handler.refreshAuthData(request, response, authenticationInfo);

        // no token user
        SlingHttpServletRequest request2 = Mockito.mock(SlingHttpServletRequest.class);
        HttpSession session2 = Mockito.mock(HttpSession.class);
        when(request2.getSession()).thenReturn(session2);
        User userNoToken = Mockito.mock(User.class);
        when(user.getID()).thenReturn("no-token-user");
        AuthenticationInfo authenticationInfoNT = handler.buildAuthInfo(userNoToken);
        handler.getStorageAuthInfo().getString(request2);
        handler.refreshAuthData(request2, response, authenticationInfoNT);

        AuthenticationInfo authenticationInfo2 = handler.buildAuthInfo(encodedToken);
        assertNotNull(authenticationInfo2);
        assertEquals(userId, authenticationInfo2.getUser());
    }

    @Test
    public void test_buildAuthInfo() throws RepositoryException {
        final AuthenticationHandlerSAML2Impl handler = new AuthenticationHandlerSAML2Impl();
        assertTrue(handler.needsRefresh(null));
        User user = Mockito.mock(User.class);
        when(user.getID()).thenReturn("test-user");
        AuthenticationInfo authenticationInfo = handler.buildAuthInfo(user);
        assertEquals(AuthenticationHandlerSAML2Impl.AUTH_TYPE, authenticationInfo.getAuthType());
        assertEquals("test-user", authenticationInfo.getUser());
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

