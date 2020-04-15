
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

package org.apache.sling.scripting.gql.engine;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/** Provides a Resource-specific GraphQL Schema, as text */
@Component(service = GraphQLSchemaProvider.class, immediate = true, property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Scripting GraphQL SchemaProvider",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation" })
public class GraphQLSchemaProvider {

    public static final String SCHEMA_EXTENSION = ".GQLschema";

    @Reference
    protected SlingRequestProcessor requestProcessor;

    public static class SchemaProviderException extends IOException {
        private static final long serialVersionUID = 1L;

        SchemaProviderException(String reason) {
            super(reason);
        }

        SchemaProviderException(String reason, Throwable cause) {
            super(reason, cause);
        }
    }

    public String getSchema(Resource r) throws Exception {
        final ResourceResolver resourceResolver = r.getResourceResolver();
        if(r == null) {
            throw new SchemaProviderException("Resource.getResourceResolver() is null");
        }
        final MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(resourceResolver);
        final String path = r.getPath() + SCHEMA_EXTENSION;
        request.setPathInfo(path);
        final MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        final int status = response.getStatus();
        if(status != 200) {
            throw new SchemaProviderException("Request to " + path + " returns HTTP status " + status);
        }
        requestProcessor.processRequest(request, response, resourceResolver);
        return response.getOutputAsString();
    }
}
