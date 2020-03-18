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
 *
 */

package org.apache.sling.auth.saml2.impl;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.saml2.sync.Saml2User;
import org.apache.sling.auth.saml2.Saml2UserMgtService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(service={Saml2UserMgtService.class}, immediate = true)
public class Saml2UserMgtServiceImpl implements Saml2UserMgtService {

    @Reference
    private ResourceResolverFactory resolverFactory;
    private ResourceResolver resourceResolver;
    private static Logger logger = LoggerFactory.getLogger(Saml2UserMgtServiceImpl.class);
    public static String SERVICE_NAME = "Saml2UserMgtService";
    public static String SERVICE_USER = "saml2-user-mgt";

    void setResourceResolver() throws org.apache.sling.api.resource.LoginException {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);
        this.resourceResolver = resolverFactory.getServiceResourceResolver(param);
    }

    ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    void closeResourceResolver(){
        this.resourceResolver.close();
    }

    @Override
    public User getOrCreateSamlUser(Saml2User user) {
        try {
            setResourceResolver();
        } catch (LoginException e) {
            logger.error("Could not get SAML2 User Service \r\n" +
                "Check mapping org.apache.sling.auth.saml2:{}={}", SERVICE_NAME, SERVICE_USER, e);
        }
        return null;
    }
}
