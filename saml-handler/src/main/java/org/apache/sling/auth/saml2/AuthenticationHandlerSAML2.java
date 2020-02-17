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

package org.apache.sling.auth.saml2;

import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(
        service = AuthenticationHandler.class ,
        name = "Saml2SPAuthenticationHandler",
        property = {"sling.servlet.methods={GET, POST}",
            AuthenticationHandler.PATH_PROPERTY+"={}",
        },
        immediate = true)

@Designate(ocd = AuthenticationHandlerSAML2Config.class)

public class AuthenticationHandlerSAML2 extends DefaultAuthenticationFeedbackHandler implements AuthenticationHandler {

    private String[] path;
    private String saml2SessAttr;
    private boolean saml2SPEnabled = false;

    @Activate
    protected void activate(final AuthenticationHandlerSAML2Config config) {
        this.path = config.path();
        this.saml2SessAttr = config.saml2SessionAttr();
        this.saml2SPEnabled = config.saml2SPEnabled();
    }

    @Override
    public AuthenticationInfo extractCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        if (this.saml2SPEnabled) {
            // Try getting credentials from the session
            if (httpServletRequest.getSession().getAttribute(this.saml2SessAttr) != null) {
                // extract credentials
                // validate credentials
            } else {


            }
        }
        return null;
    }

    @Override
    public boolean requestCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        return false;
    }

    @Override
    public void dropCredentials(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {

    }
}
