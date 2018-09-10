/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.oidchandler.core.configuration;

import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

import javax.servlet.Servlet;

@Component( immediate = true, service = Servlet.class)
@Designate(ocd = OIDCConfiguration.class)
public class OIDCConfigServlet extends SlingSafeMethodsServlet {

    private static OIDCConfiguration config;

    @Activate
    protected void Activate(OIDCConfiguration config) {
        this.config = config;

    }

    public static Boolean getOIDCEnabled(){
        return config.oidc_enable_boolean();
    }

    public static String getClientID(){
        return config.clientid_string();
    }

    public static String getClientSecret(){
        return config.clientsecret_password();
    }

    public static String getCallbackURL(){
        return config.oidc_callback_url_string();
    }

    public static String getOIDCScope(){
        return config.oidc_scope_string();
    }

    public static String getIssuerURL(){
        return config.issuer_url_string();
    }

    public static String getAuthEndpoint(){
        return config.authorization_endpoint_string();
    }

    public static String getTokenEndpoint(){
        return config.token_endpoint_string();
    }

    public static String getUserinfoEndpoint(){
        return config.userinfo_endpoint_string();
    }

    public static String getRevocationEndpoint(){
        return config.revocation_endpoint_string();
    }

    public static String getJwksUri(){
        return config.jwks_uri_string();
    }

}
