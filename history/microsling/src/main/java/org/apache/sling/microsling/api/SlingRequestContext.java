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
package org.apache.sling.microsling.api;

import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.microsling.api.exceptions.SlingException;
import org.apache.sling.microsling.api.requestparams.RequestParameterMap;
import org.apache.sling.microsling.helpers.exceptions.MissingRequestAttributeException;

/** Provides Sling-specific information about the current Request.
 *  Can be acquired using getFromRequest(...).
 */
public abstract class SlingRequestContext {

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    protected final ServletContext servletContext;
    
    /** Attribute name used to store this in the request */
    protected final static String REQ_ATTR_NAME = SlingRequestContext.class.getName();
    
    /** Get the SlingRequestContext that's stored in req as an attribute */
    public static SlingRequestContext getFromRequest(HttpServletRequest req) 
    throws SlingException {
        final SlingRequestContext ctx = (SlingRequestContext)req.getAttribute(REQ_ATTR_NAME);
        if(ctx == null) {
            throw new MissingRequestAttributeException(REQ_ATTR_NAME);
        }
        return ctx;
    }
    
    /** Create a SlingRequestContext and store it as a request attribute */
    public SlingRequestContext(ServletRequest req, ServletResponse resp, ServletContext sctx) throws SlingException {
        this.request = (HttpServletRequest)req;
        this.response = (HttpServletResponse)resp;
        this.servletContext = sctx;
        request.setAttribute(REQ_ATTR_NAME,this);
    }
    
    /** The ServiceLocator provides access to other services */
    public abstract ServiceLocator getServiceLocator() throws SlingException;

    /** Acquire a JCR Session if not done yet, and return it */
    public abstract Session getSession() throws SlingException;

    /** Get the Resource that the current Request processes, null if not found */
    public abstract Resource getResource() throws SlingException;

    /**  The preferred Content-Type to use for the response */
    public abstract String getPreferredResponseContentType() throws SlingException;
    
    /** The possible Content-Type values for the response, in order of preference */
    public abstract String [] getResponseContentTypes() throws SlingException;

    /** Return the SlingRequestPathInfo for this request: selectors, extension, suffix, etc. */
    public abstract SlingRequestPathInfo getRequestPathInfo() throws SlingException;

    /** Return the map of request parameters */
    public abstract RequestParameterMap getRequestParameterMap() throws SlingException;
}
