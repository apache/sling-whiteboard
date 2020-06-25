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
    private boolean saml2SPEncryptAndSign = false;
    private String uidAttrName;
    private String samlUserHome;
    private String groupMembershipName;
    private String entityID;
    private String jksFileLocation;
    private String jksStorePassword;
    private String spKeysAlias;
    private String spKeysPassword;
    private String idpCertAlias;
    private String acsPath;
    private String[] syncAttrs;
    private String saml2LogoutURL;

    public static final String GOTO_URL_SESSION_ATTRIBUTE = "gotoURL";
    public static final String SAML2_REQUEST_ID = "saml2RequestID";
    public static final String AUTHENTICATED_SESSION_ATTRIBUTE = "authenticated";

    @Activate
    protected void activate(final AuthenticationHandlerSAML2Config config, ComponentContext componentContext) {
        this.path = config.path();
        this.saml2SessAttr = config.saml2SessionAttr();
        this.saml2SPEnabled = config.saml2SPEnabled();
        this.saml2SPEncryptAndSign = config.saml2SPEncryptAndSign();
        this.saml2IDPDestination = config.saml2IDPDestination();
        this.uidAttrName = config.saml2userIDAttr();
        this.samlUserHome = config.saml2userHome();
        this.groupMembershipName = config.saml2groupMembershipAttr();
        this.entityID = config.entityID();
        this.jksFileLocation = config.jksFileLocation();
        this.jksStorePassword = config.jksStorePassword();
        this.spKeysAlias = config.spKeysAlias();
        this.spKeysPassword = config.spKeysPassword();
        this.idpCertAlias = config.idpCertAlias();
        this.acsPath = config.acsPath();
        this.syncAttrs = config.syncAttrs();
        this.saml2LogoutURL = config.saml2LogoutURL();
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
    public String getSaml2userHome() {
        return this.samlUserHome;
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
    public String getEntityID() {
        return this.entityID;
    }

    @Override
    public String getAcsPath() {
        return this.acsPath;
    }

    @Override
    public boolean getSaml2SPEnabled() {
        return this.saml2SPEnabled;
    }

    @Override
    public boolean getSaml2SPEncryptAndSign() {
        return this.saml2SPEncryptAndSign;
    }

    @Override
    public String getSaml2LogoutURL() {
        return this.saml2LogoutURL;
    }

    @Override
    public String getJksFileLocation() {
        return this.jksFileLocation;
    }

    @Override
    public String getJksStorePassword() {
        return this.jksStorePassword;
    }

    @Override
    public String getSpKeysAlias() {
        return this.spKeysAlias;
    }

    @Override
    public String getSpKeysPassword() {
        return this.spKeysPassword;
    }

    @Override
    public String getIdpCertAlias() {
        return this.idpCertAlias;
    }

    @Override
    public String[] getSyncAttrs() {
        return this.syncAttrs;
    }

    @Override
    public String getACSURL() {
        final String domain = entityID.endsWith("/") ? entityID.substring(0, entityID.length()-1) : entityID;
        return domain + this.getAcsPath();
    }
}
