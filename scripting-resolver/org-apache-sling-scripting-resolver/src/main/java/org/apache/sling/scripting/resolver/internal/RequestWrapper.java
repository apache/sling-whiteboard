/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.resolver.internal;

import java.util.Set;

import javax.servlet.RequestDispatcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

class RequestWrapper extends SlingHttpServletRequestWrapper {

    private final Set<String> wiredResourceTypes;

    RequestWrapper(SlingHttpServletRequest wrappedRequest, Set<String> wiredResourceTypes) {
        super(wrappedRequest);
        this.wiredResourceTypes = wiredResourceTypes;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options) {
        if (resource == null) {
            return null;
        }
        if (StringUtils.isEmpty(options.getForceResourceType())) {
            if (!StringUtils.isEmpty(resource.getResourceType())) {
                options.setForceResourceType(resource.getResourceType());
            } else if (!StringUtils.isEmpty(resource.getResourceSuperType())) {
                options.setForceResourceType(resource.getResourceSuperType());
            }
        }
        RequestDispatcherOptions processedOptions = processOptions(options);
        return super.getRequestDispatcher(resource, processedOptions);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options) {
        if (path == null) {
            return null;
        }
        RequestDispatcherOptions processedOptions = processOptions(options);
        return super.getRequestDispatcher(path, processedOptions);
    }

    private RequestDispatcherOptions processOptions(RequestDispatcherOptions options) {
        if (options != null) {
            RequestDispatcherOptions requestDispatcherOptions = new RequestDispatcherOptions();
            requestDispatcherOptions.setForceResourceType(options.getForceResourceType());
            requestDispatcherOptions.setAddSelectors(options.getAddSelectors());
            requestDispatcherOptions.setReplaceSelectors(options.getReplaceSelectors());
            requestDispatcherOptions.setReplaceSuffix(options.getReplaceSuffix());
            String forcedResourceType = options.getForceResourceType();
            if (StringUtils.isNotEmpty(forcedResourceType)) {
                for (String wiredResourceType : wiredResourceTypes) {
                    if (wiredResourceType.startsWith(forcedResourceType + "/")) {
                        requestDispatcherOptions.setForceResourceType(wiredResourceType);
                        break;
                    }
                }
            }
            return requestDispatcherOptions;
        }
        return null;
    }
}
