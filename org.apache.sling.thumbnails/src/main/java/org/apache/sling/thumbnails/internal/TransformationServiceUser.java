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
package org.apache.sling.thumbnails.internal;

import java.util.Collections;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Service for retrieving the service user
 */
@Component(service = TransformationServiceUser.class)
public class TransformationServiceUser {

    public static final String SERVICE_USER = "sling-thumbnails";

    private final ResourceResolverFactory resolverFactory;

    @Activate
    public TransformationServiceUser(@Reference ResourceResolverFactory resolverFactory) {
        this.resolverFactory = resolverFactory;
    }

    public ResourceResolver getTransformationServiceUser() throws LoginException {
        return resolverFactory
                .getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_USER));
    }

}
