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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


/**
 * The configuration for <code>SAML2</code> in Apache Sling
 *
 * @see AuthenticationHandlerSAML2
 */

@ObjectClassDefinition(name="SAML2 Service Provider (SP) Configuration",
        description = "Configure SAML SSO by configuring details about your Identify Provider (IdP)"+
                "and related Service Provider metadata")
public @interface AuthenticationHandlerSAML2Config {

    @AttributeDefinition(name = "Path",
            description="One or more URL paths (String) for which this AuthenticationHandler is applied")
    String[] path() default {"http://localhost:8080/"};

    @AttributeDefinition(name = "User ID (uid) Attribute Name",
        description="Name of the attribute holding the users unique id")
    String saml2userIDAttr() default "username";

    @AttributeDefinition(name = "Path for SAML2 Users",
            description="Home path for SAML2 Users")
    String saml2userHome() default "/home/users/saml";

    @AttributeDefinition(name = "groupMembership Attribute Name",
            description="Name of the attribute holding the users' group memberships")
    String saml2groupMembershipAttr() default "";

    @AttributeDefinition(name = "SAML2 Session Attribute",
            description="Name used to save the users security context within a HTTP SESSION")
    String saml2SessionAttr() default "saml2AuthInfo";

    @AttributeDefinition(name = "SAML2 IDP Destination",
            description="")
    String saml2IDPDestination() default "http://localhost:8080/idp/profile/SAML2/POST/SSO";

    @AttributeDefinition(
            name = "Enabled",
            description = " SAML2 Authentication Handler Enabled",
            type = AttributeType.BOOLEAN
    )
    boolean saml2SPEnabled() default false;
}
