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

package org.apache.sling.rtdx.impl;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.rtdx.api.RtdxConstants;
import org.osgi.service.component.annotations.Component;

/** Servlet Filter that provides the correct redirects when creating
 *  Resources from RTD-X forms.
 */
@Component(service = Filter.class,
    property = {
            "service.description=RTD-X Redirect Filter",
            "service.vendor=The Apache Software Foundation",
            "sling.filter.scope=request"
    })
public class RedirectFilter implements Filter {

    public static final String LOCATION_HEADER = "Location";
    
    class RtdxResponseWrapper extends SlingHttpServletResponseWrapper {
        RtdxResponseWrapper(SlingHttpServletResponse r) {
            super(r);
        }

        @Override
        public void setHeader(String name, String value) {
            if(LOCATION_HEADER.equals(name)) {
                value = transformRedirectUrl(value);
            }
            super.setHeader(name, value);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            super.sendRedirect(transformRedirectUrl(location));
        }
        
        private String transformRedirectUrl(String url) {
            return url + "." + RtdxConstants.RTDX_SELECTOR + "." + RtdxConstants.EXT_HTML;
        }
    }
            
    private boolean hasSelector(SlingHttpServletRequest request, String selector) {
        for(String s : request.getRequestPathInfo().getSelectors()) {
            if(s.equals(selector)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(request instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest sr = (SlingHttpServletRequest)request;
            if(sr.getMethod().equals(RtdxConstants.POST_METHOD) && sr.getParameter(RtdxConstants.RTDX_FORM_MARKER_PARAMETER) != null) {
                response = new RtdxResponseWrapper((SlingHttpServletResponse)response);
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}