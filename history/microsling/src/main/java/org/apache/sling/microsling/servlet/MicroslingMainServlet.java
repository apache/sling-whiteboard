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
package org.apache.sling.microsling.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.microsling.api.Resource;
import org.apache.sling.microsling.api.ServiceLocator;
import org.apache.sling.microsling.api.SlingRequestContext;
import org.apache.sling.microsling.api.exceptions.HttpStatusCodeException;
import org.apache.sling.microsling.api.exceptions.SlingException;
import org.apache.sling.microsling.contenttype.ResponseContentTypeResolverFilter;
import org.apache.sling.microsling.request.MicroslingRequestContext;
import org.apache.sling.microsling.scripting.SlingScriptResolver;
import org.apache.sling.microsling.services.MicroslingServiceLocator;
import org.apache.sling.microsling.slingservlets.DefaultSlingServlet;
import org.apache.sling.microsling.slingservlets.StreamServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main microsling servlet: apply Filters to the request using our
 * MicroSlingFilterHelper, select and delegate to a SlingServlet to process the
 * request.
 */
public class MicroslingMainServlet extends GenericServlet {

    private static final long serialVersionUID = 1L;

    private MicroSlingFilterHelper filterChain;

    private ServiceLocator serviceLocator;

    private Map<String, Servlet> servlets;

    private SlingScriptResolver scriptResolver;

    private DefaultSlingServlet defaultSlingServlet;
    
    private static final Logger log = LoggerFactory.getLogger(MicroslingMainServlet.class);

    @Override
    public void init() throws ServletException {
        super.init();
        servlets = new HashMap<String, Servlet>();
        initFilterChain();
        initServlets();
        initServiceLocator();
        initScriptResolver();
    }
    
    /** init our filter chain */
    protected void initFilterChain() throws ServletException {
        filterChain = new MicroSlingFilterHelper(this);
        addFilter(new ResponseContentTypeResolverFilter());
    }
    
    /** init our servlets */
    protected void initServlets() throws ServletException {
        // TODO use a utility class to map nt:file to the magic NODETYPES path 
        addServlet("NODETYPES/nt/file", new StreamServlet());
        defaultSlingServlet = new DefaultSlingServlet();
    }

    /** init our serviceLocator */
    protected void initServiceLocator() throws ServletException {
        serviceLocator = new MicroslingServiceLocator();
    }
    
    /** init our scriptResolver */
    protected void initScriptResolver() throws ServletException {
        scriptResolver = new SlingScriptResolver();
    }
    
    /**
     * Execute our Filters via MicroSlingFilterHelper, which calls our doService
     * method after executing the filters
     */
    public void service(ServletRequest req, ServletResponse resp)
            throws ServletException, IOException {

        // our filters might need the SlingRequestContext to store info in it
        new MicroslingRequestContext(getServletContext(), req, resp, serviceLocator);
        filterChain.service(req, resp);
    }

    @Override
    public void destroy() {
        // just for completeness, we have to take down our filters
        if (filterChain != null) {
            filterChain.destroy();
            filterChain = null;
        }

        // destroy registered servlets
        Servlet[] servletList = servlets.values().toArray(
            new Servlet[servlets.size()]);
        for (Servlet servlet : servletList) {
            try {
                servlet.destroy();
            } catch (Throwable t) {
                getServletContext().log(
                    "Unexpected problem destroying servlet " + servlet, t);
            }
        }
        servlets.clear();

        // destroy base class at the end
        super.destroy();
    }

    /**
     * Called by
     * {@link MicroSlingFilterHelper#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
     * after all filters have been processed.
     */
    void doService(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            // Select a SlingServlet and delegate the actual request processing
            // to it
            final Servlet selectedServlet = selectSlingServlet(req);
            if (selectedServlet != null) {
                delegateToSlingServlet(selectedServlet, req, resp);
            } else {
                // no typed servlet, so lets try scripting
                boolean scriptExists = scriptResolver.evaluateScript(req, resp);
                if (!scriptExists) {
                    if(log.isDebugEnabled()) {
                        final SlingRequestContext ctx = SlingRequestContext.getFromRequest(req);
                        final Resource r = ctx.getResource();
                        log.debug("No specific Servlet or script found for Resource " + r + ", using default Servlet");
                    }
                    delegateToSlingServlet(defaultSlingServlet, req, resp);
                }
            }

        } catch (HttpStatusCodeException hts) {
            resp.sendError(hts.getStatusCode(), hts.getMessage());

        } catch (Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            resp.sendError(500, e.getMessage() + "\n" + sw.toString());
        }
    }

    /** Select a SlingServlet to process the given request */
    protected Servlet selectSlingServlet(HttpServletRequest req)
            throws SlingException {

        // use the resource type to select a servlet
        final SlingRequestContext ctx = SlingRequestContext.getFromRequest(req);
        final Resource r = ctx.getResource(); 
        String type = (r == null ? null : r.getResourceType());
        final Servlet result = (type != null) ? servlets.get(type) : null;
        
        if(log.isDebugEnabled()) {
            if(result==null) {
                log.debug("No Servlet found for resource type " + type);
            } else {
                log.debug("Using Servlet class " + result.getClass().getSimpleName() + " for resource type " + type);
             }
        }
        
        return result;
    }

    /** Delegate to the given SlingServlet, based on the request HTTP method */
    protected void delegateToSlingServlet(Servlet s, HttpServletRequest req,
            HttpServletResponse resp) throws ServletException, IOException {
        s.service(req, resp);
    }

    /** Add a filter to our MicroSlingFilterHelper */
    protected void addFilter(Filter filter) throws ServletException {
        filterChain.addFilter(filter);
    }

    /** Add servlets by resource type */
    protected void addServlet(final String resourceType, Servlet servlet) {

        try {
            ServletConfig config = new ServletConfig() {
                public String getInitParameter(String name) {
                    return MicroslingMainServlet.this.getInitParameter(name);
                }
                public Enumeration<?> getInitParameterNames() {
                    return MicroslingMainServlet.this.getInitParameterNames();
                }
                public ServletContext getServletContext() {
                    return MicroslingMainServlet.this.getServletContext();
                }
                public String getServletName() {
                    return resourceType;
                }
            };
            servlet.init(config);

            // only register if initialization succeeds
            servlets.put(resourceType, servlet);
        } catch (Throwable t) {
            getServletContext().log("Failed initializing servlet " + servlet + " for type " + resourceType, t);
        }

    }
}
