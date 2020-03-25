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
package org.apache.sling.auth.saml2.sync;

import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentityException;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalIdentityRef;
import org.apache.jackrabbit.oak.spi.security.authentication.external.ExternalUser;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Saml2User implements ExternalUser {
    private String id;
    private HashMap userProperties;
    private HashSet groupMembership;

    public Saml2User(){
        userProperties = new HashMap<String, Object>();
        groupMembership = new HashSet<String>();
    }

    public Saml2User(String id){
        this();
        this.id = id;
    }

    @Nonnull
    @Override
    public ExternalIdentityRef getExternalId() {
        return null;
    }

    public String getId() {
        return id;
    }

    @Nonnull
    @Override
    public String getPrincipalName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getIntermediatePath() {
        return null;
    }

    @Nonnull
    @Override
    public Iterable<ExternalIdentityRef> getDeclaredGroups() throws ExternalIdentityException {
        return null;
    }

    @Nonnull
    @Override
    public Map<String, ?> getProperties() {
        return null;
    }

    public Map getUserProperties() {
        return userProperties;
    }

    public Set getGroupMembership() {
        return groupMembership;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void addUserProperty(String key, Object value) {
        this.userProperties.put(key, value);
    }

    public void addGroupMembership(String group) {
        this.groupMembership.add(group);
    }
}
