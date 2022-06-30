/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jaxrs.sample;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.commons.mime.MimeTypeService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Context that registers an HTTP whiteboard for "/test".
 */
@Component(service = ServletContextHelper.class, property = {
        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + TestHttpContext.CONTEXT_NAME,
        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=" + TestHttpContext.CONTEXT_PATH
})
public class TestHttpContext extends ServletContextHelper {

    public static final String CONTEXT_NAME = "org.apache.sling.test";
    public static final String CONTEXT_PATH = "/test";

    private final MimeTypeService mimeTypeService;

    private final AuthenticationSupport authenticationSupport;

    /**
     * Constructs a new context that will use the given dependencies.
     *
     * @param mimeTypeService       Used when providing mime type of requests.
     * @param authenticationSupport Used to authenticate requests with sling.
     */
    @Activate
    public TestHttpContext(@Reference final MimeTypeService mimeTypeService,
            @Reference final AuthenticationSupport authenticationSupport) {
        this.mimeTypeService = mimeTypeService;
        this.authenticationSupport = authenticationSupport;
    }

    // ---------- HttpContext interface ----------------------------------------
    /**
     * Returns the MIME type as resolved by the <code>MimeTypeService</code> or
     * <code>null</code> if the service is not available.
     */
    @Override
    public String getMimeType(String name) {
        MimeTypeService mtservice = mimeTypeService;
        if (mtservice != null) {
            return mtservice.getMimeType(name);
        }
        return null;
    }

    /**
     * Returns the real context path that is used to mount this context.
     *
     * @param req servlet request
     * @return the context path
     */
    public static String getRealContextPath(HttpServletRequest req) {
        final String path = req.getContextPath();
        if (path.equals(CONTEXT_PATH)) {
            return "";
        }
        return path.substring(CONTEXT_PATH.length());
    }

    /**
     * Returns a request wrapper that transforms the context path back to the
     * original one
     *
     * @param req request
     * @return the request wrapper
     */
    public static HttpServletRequest createContextPathAdapterRequest(HttpServletRequest req) {
        return new HttpServletRequestWrapper(req) {

            @Override
            public String getContextPath() {
                return getRealContextPath((HttpServletRequest) getRequest());
            }

        };

    }

    /**
     * Always returns <code>null</code> because resources are all provided
     * through individual endpoint implementations.
     */
    @Override
    public URL getResource(String name) {
        return null;
    }

    /**
     * Tries to authenticate the request using the
     * <code>SlingAuthenticator</code>. If the authenticator or the Repository
     * is missing this method returns <code>false</code> and sends a 503/SERVICE
     * UNAVAILABLE status back to the client.
     */
    @Override
    public boolean handleSecurity(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        final AuthenticationSupport authenticator = this.authenticationSupport;
        if (authenticator != null) {
            return authenticator.handleSecurity(createContextPathAdapterRequest(request), response);
        }

        // send 503/SERVICE UNAVAILABLE, flush to ensure delivery
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "AuthenticationSupport service missing. Cannot authenticate request.");
        response.flushBuffer();

        // terminate this request now
        return false;
    }
}
