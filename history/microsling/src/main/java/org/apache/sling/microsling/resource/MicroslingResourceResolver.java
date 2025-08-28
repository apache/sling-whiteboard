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
package org.apache.sling.microsling.resource;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.microsling.api.Resource;
import org.apache.sling.microsling.api.ResourceMetadata;
import org.apache.sling.microsling.api.ResourceResolver;
import org.apache.sling.microsling.api.SlingRequestContext;
import org.apache.sling.microsling.api.exceptions.SlingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The microsling ResourceResolver locates a Resource using a ResourcePathIterator:
 *  it first tries to find an exact match for the Request URI, and if not goes up
 *  the path, breaking it a "." and "/", stopping if it finds a Resource. 
 */
public class MicroslingResourceResolver implements ResourceResolver {

    private static final Logger log = LoggerFactory.getLogger(MicroslingResourceResolver.class);    
            
    public Resource getResource(HttpServletRequest request) throws SlingException {
        final SlingRequestContext ctx = SlingRequestContext.getFromRequest(request);
        Resource result = null;
        String path = null;

        try {
            final ResourcePathIterator it = new ResourcePathIterator(request.getPathInfo());
            while(it.hasNext() && result == null) {
                path = it.next();
                if(log.isDebugEnabled()) {
                    log.debug("Trying to locate Resource at path '" + path + "'");
                }
                if (ctx.getSession().itemExists(path)) {
                    result = new JcrNodeResource(ctx.getSession(),path);
                    result.getMetadata().put(ResourceMetadata.RESOLUTION_PATH, path);
                    if(log.isInfoEnabled()) {
                        log.info("Found Resource at path '" + path + "'");
                    }
                }
            }
        } catch(RepositoryException re) {
            throw new SlingException("RepositoryException for path=" + path,re);
        }
        
        return result;
    }
}
