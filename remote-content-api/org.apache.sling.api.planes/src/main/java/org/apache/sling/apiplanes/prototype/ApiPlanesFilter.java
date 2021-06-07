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

package org.apache.sling.apiplanes.prototype;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.servlets.annotations.SlingServletFilter;
import org.apache.sling.servlets.annotations.SlingServletFilterScope;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@SlingServletFilter(scope = {SlingServletFilterScope.REQUEST})
public class ApiPlanesFilter implements Filter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // TODO make this configurable
    private final char[] apiPlaneSelectors = { 'N', 'D' };
    public static final String API_PLANE_RESOURCE_TYPE_PREFIX = "/sling/APIplane";
    private static final String SLASH ="/";

    @Override
    public void doFilter(ServletRequest rawRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletRequest requestToProcess = rawRequest;
        if(rawRequest instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest request = (SlingHttpServletRequest)rawRequest;
            final Optional<Character> apiPlane = getPlaneSelector(request.getRequestPathInfo().getSelectors());
            if(apiPlane.isPresent()) {
                final String actualResourceType = request.getResource().getResourceType();
                final String separator = actualResourceType.startsWith(SLASH) ? "" : SLASH;
                final String replacementResourceSuperType = String.format("%s/%c", 
                    API_PLANE_RESOURCE_TYPE_PREFIX, apiPlane.get());
                final String replacementResourceType = String.format("%s%s%s", 
                    replacementResourceSuperType, separator, actualResourceType);
                requestToProcess = getWrappedRequest(request, apiPlane.get(), replacementResourceType, replacementResourceSuperType);
            }
        }
        chain.doFilter(requestToProcess, response);
    }

    private Optional<Character> getPlaneSelector(String [] selectors) {
        if(selectors.length >= 1 && selectors[0].length() == 1) {
            final char s = selectors[0].charAt(0);
            for(char p : apiPlaneSelectors) {
                if(s == p) {
                    return Optional.of(s);
                }
            }
        }
        return Optional.empty();
    }

    /** Wrap the supplied request to change its resource type */
    private SlingHttpServletRequest getWrappedRequest(SlingHttpServletRequest toWrap, Character apiPlane, final String replacementResourceType, String replacementResourceSuperType) {
        log.info("Wrapping request to {} for API plane {}, with resource type {} and supertype {}", 
            toWrap.getResource().getPath(), apiPlane, replacementResourceType, replacementResourceSuperType);
        final Resource wrappedResource = new ResourceWrapper(toWrap.getResource()) {
            @Override
            public String getResourceType() {
                return replacementResourceType;
            }
            @Override
            public String getResourceSuperType() {
                return replacementResourceSuperType;
            }
        };
        return new SlingHttpServletRequestWrapper(toWrap) {
            @Override
            public Resource getResource() {
                return wrappedResource;
            }
        };
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to init
    }

    @Override
    public void destroy() {
        // nothing to destroy
    }
}
