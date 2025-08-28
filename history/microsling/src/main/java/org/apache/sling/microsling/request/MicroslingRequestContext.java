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
package org.apache.sling.microsling.request;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.microsling.api.Resource;
import org.apache.sling.microsling.api.ResourceResolver;
import org.apache.sling.microsling.api.ServiceLocator;
import org.apache.sling.microsling.api.SlingRequestContext;
import org.apache.sling.microsling.api.SlingRequestPathInfo;
import org.apache.sling.microsling.api.exceptions.SlingException;
import org.apache.sling.microsling.api.requestparams.RequestParameterMap;
import org.apache.sling.microsling.request.helpers.MicroslingRequestPathInfo;
import org.apache.sling.microsling.request.helpers.SlingRequestParameterMap;

/** Additional Request-based info used by for SlingServlets */
public class MicroslingRequestContext extends SlingRequestContext {

    private Session session;
    private String responseContentType;
    private final Repository repository;
    private Resource resource;
    private MicroslingRequestPathInfo requestPathInfo;
    private final ServiceLocator serviceLocator;
    private RequestParameterMap parameterMap;

    /** Build a MicroslingRequestContext and add it to the request attributes */
    public MicroslingRequestContext(ServletContext sctx,ServletRequest req,ServletResponse resp,ServiceLocator serviceLocator) 
    throws SlingException {
        super(req,resp,sctx);
        this.serviceLocator = serviceLocator;

        // Access our Repository
        final String repoAttr = Repository.class.getName();
        repository = (Repository)sctx.getAttribute(repoAttr);
        if(repository==null) {
            throw new SlingException("Repository not available in ServletContext attribute " + repoAttr);
        }

        // load the request parameter map
        // this can be done lazily when we keep the request here
        parameterMap = new SlingRequestParameterMap(request);
    }

    /** Acquire a JCR Session if not done yet, and return it */
    public Session getSession() throws SlingException {
        if(session==null) {
            try {
                session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            } catch(RepositoryException re) {
                throw new SlingException("Repository.login() failed: " + re.getMessage(),re);
            }
        }
        return session;
    }

    public Resource getResource() throws SlingException {
        if(resource==null) {
            final ResourceResolver rr = (ResourceResolver)serviceLocator.getRequiredService(ResourceResolver.class.getName());
            resource = rr.getResource(request);
        }
        return resource;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    /** Use the ServletContext to compute the mime-type for given filename */
    public String getMimeType(String filename) {
        return servletContext.getMimeType(filename);
    }

    public SlingRequestPathInfo getRequestPathInfo() throws SlingException {
        if(requestPathInfo == null) {
            // make sure Resource is resolved, as we need it to compute the path info
            getResource();
            requestPathInfo = new MicroslingRequestPathInfo(resource,request.getPathInfo());
        }
        return requestPathInfo;
    }

    /** Return the map of request parameters */
    public RequestParameterMap getRequestParameterMap() {
        return parameterMap;
    }
    
    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    @Override
    public String getPreferredResponseContentType() throws SlingException {
        return responseContentType;
    }

    @Override
    /** microsling only initializes the first value in the returned array */
    public String[] getResponseContentTypes() throws SlingException {
        return new String [] { responseContentType };
    }

}
