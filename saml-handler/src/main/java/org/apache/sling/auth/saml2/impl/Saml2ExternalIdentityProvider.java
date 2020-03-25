/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * with the License.  You may obtain a copy of the License at
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 */

package org.apache.sling.auth.saml2.impl;

import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.authentication.external.*;
import org.apache.sling.auth.saml2.sync.Saml2User;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.jcr.Credentials;
import javax.security.auth.login.LoginException;
import java.util.Iterator;
import java.util.Map;

/**
 * Derived works from
 * https://github.com/peregrine-cms/peregrinecms-com-peregrine-sling-auth-header/blob/master/src/main/java/com/peregrine/sling/auth/header/HeaderExternalIdentityProvider.java
 */

@Component(
        name = "SAML2 External Identity Provider",
        service = ExternalIdentityProvider.class,
        immediate = true
)
public class Saml2ExternalIdentityProvider implements ExternalIdentityProvider {

    private final Logger logger = LoggerFactory.getLogger(Saml2ExternalIdentityProvider.class);
    public static final String NAME = "SAML2";

    public Saml2ExternalIdentityProvider(){
        // Default constructor
    }

    @Activate
    public void activate(Map<String, Object> properties)
    {
        ConfigurationParameters config = ConfigurationParameters.of(properties);
        logger.info("Activated IDP: '{}' with config: '{}'", getName(), config);
    }

    @Nonnull
    @Override
    public String getName() {
        return NAME;
    }

    @CheckForNull
    @Override
    public ExternalIdentity getIdentity(@Nonnull ExternalIdentityRef externalIdentityRef) throws ExternalIdentityException {
        if (getName().equals(externalIdentityRef.getProviderName()))
        {
            String id = externalIdentityRef.getId();
            return getUser(id);
        } else
        {
            return null;
        }
    }

    @CheckForNull
    @Override
    public ExternalUser getUser(@Nonnull final String s) throws ExternalIdentityException {
        new Saml2User(s);
        return null;
    }

    @CheckForNull
    @Override
    public ExternalUser authenticate(@Nonnull Credentials credentials) throws ExternalIdentityException, LoginException {
        if (credentials instanceof Saml2Credentials)
        {
            String userId = ((Saml2Credentials) credentials).getUserId();
            return getUser(userId);
        } else
        {
            throw new LoginException("Unsupported credentials");
        }
    }

    @CheckForNull
    @Override
    public ExternalGroup getGroup(@Nonnull String s) throws ExternalIdentityException {
        throw new UnsupportedOperationException("getGroup");
    }

    @Nonnull
    @Override
    public Iterator<ExternalUser> listUsers() throws ExternalIdentityException {
        throw new UnsupportedOperationException("listUsers");
    }

    @Nonnull
    @Override
    public Iterator<ExternalGroup> listGroups() throws ExternalIdentityException {
        throw new UnsupportedOperationException("listGroups");
    }
}
