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
package org.apache.sling.auth.saml2.impl;

import org.apache.sling.auth.saml2.AuthenticationHandlerSAML2Config;
import org.apache.sling.auth.saml2.SAML2ConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

@Component(service={SAML2ConfigService.class}, immediate = true)
@Designate(ocd = AuthenticationHandlerSAML2Config.class)

public class SAML2ConfigServiceImpl implements SAML2ConfigService {
    // OSGI Configs
    private String[] path;
    private String saml2SessAttr;
    private String saml2IDPDestination;
    private boolean saml2SPEnabled = false;
    private String uidAttrName;
    private String groupMembershipName;

    @Activate
    protected void activate(final AuthenticationHandlerSAML2Config config, ComponentContext componentContext) {
        this.path = config.path();
        this.saml2SessAttr = config.saml2SessionAttr();
        this.saml2SPEnabled = config.saml2SPEnabled();
        this.saml2IDPDestination = config.saml2IDPDestination();
        this.uidAttrName = config.saml2userIDAttr();
        this.groupMembershipName = config.saml2groupMembershipAttr();
    }


    @Override
    public String[] getSaml2Path() {
        return this.path;
    }

    @Override
    public String getSaml2userIDAttr() {
        return this.uidAttrName;
    }

    @Override
    public String getSaml2groupMembershipAttr() {
        return this.groupMembershipName;
    }

    @Override
    public String getSaml2SessionAttr() {
        return this.saml2SessAttr;
    }

    @Override
    public String getSaml2IDPDestination() {
        return this.saml2IDPDestination;
    }

    @Override
    public boolean getSaml2SPEnabled() {
        return this.saml2SPEnabled;
    }
}
