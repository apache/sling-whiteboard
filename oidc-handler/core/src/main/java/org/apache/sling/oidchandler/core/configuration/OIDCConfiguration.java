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

import org.apache.sling.oidchandler.core.util.OPConstant;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.apache.commons.lang3.StringUtils;

@ObjectClassDefinition(name = "Google - OpenID Connect Configuration")
public @interface OIDCConfiguration {

    @AttributeDefinition(
            name = "Enable",
            description = "Enable OpenID Connect Authentication Handler",
            type = AttributeType.BOOLEAN
    )
    boolean oidc_enable_boolean() default true;

    @AttributeDefinition(
            name = "Client ID",
            description = "Enter Client ID",
            type = AttributeType.STRING

    )
    String clientid_string() default StringUtils.EMPTY;

    @AttributeDefinition(
            name = "Client Secret",
            description = "Enter Client Secret",
            type = AttributeType.PASSWORD
    )
    String clientsecret_password() default StringUtils.EMPTY;

    @AttributeDefinition(
            name = "Callback URL",
            description = "Enter Callback URL",
            type = AttributeType.STRING
    )
    String oidc_callback_url_string() default OPConstant.oidc_callback_url;

    @AttributeDefinition(
            name = "Scope",
            description = "Enter scope",
            type = AttributeType.STRING
    )
    String oidc_scope_string() default OPConstant.oidc_scope;


    @AttributeDefinition(
            name = "Issure",
            description = "Issure Url of Google",
            type = AttributeType.STRING
    )
    String issuer_url_string() default OPConstant.google_issuer_url;

    @AttributeDefinition(
            name = "Authorization Endpoint",
            description = "Authorization Endpoint Url of Google",
            type = AttributeType.STRING
    )
    String authorization_endpoint_string() default OPConstant.google_authorization_endpoint;

    @AttributeDefinition(
            name = "Token Endpoint",
            description = "Token Endpoint Url of Google",
            type = AttributeType.STRING
    )
    String token_endpoint_string() default OPConstant.google_token_endpoint;

    @AttributeDefinition(
            name = "Userinfo Endpoint",
            description = "Userinfo Endpoint Url of Google",
            type = AttributeType.STRING
    )
    String userinfo_endpoint_string() default OPConstant.google_userinfo_endpoint;

    @AttributeDefinition(
            name = "Revocation Endpoint",
            description = "Revocation Endpoint Url of Google",
            type = AttributeType.STRING
    )
    String revocation_endpoint_string() default OPConstant.google_revocation_endpoint;

    @AttributeDefinition(
            name = "JWKS URI",
            description = "JWKS URI of Google",
            type = AttributeType.STRING
    )
    String jwks_uri_string() default OPConstant.google_jwks_uri;

}