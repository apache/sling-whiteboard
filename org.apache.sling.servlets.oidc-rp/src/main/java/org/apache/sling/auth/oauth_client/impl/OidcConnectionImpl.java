/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.auth.oauth_client.impl;

import org.apache.sling.auth.oauth_client.ClientConnection;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

// TODO - bad name
@Component
@Designate(ocd = OidcConnectionImpl.Config.class, factory = true)
public class OidcConnectionImpl implements ClientConnection {

    @ObjectClassDefinition(name = "OpenID Connect connection details")
    public @interface Config {
        String name();
        String baseUrl();
        String clientId();
        @AttributeDefinition(type = AttributeType.PASSWORD) String clientSecret();
        String[] scopes();
        String[] additionalAuthorizationParameters();
        
        String webconsole_configurationFactory_nameHint() default "Name: {name}, base URL: {baseUrl}, clientId: {clientId}";
    }

    private final Config cfg;
    private final OidcProviderMetadataRegistry metadataRegistry;

    @Activate
    public OidcConnectionImpl(Config cfg, @Reference OidcProviderMetadataRegistry metadataRegistry) {
        this.cfg = cfg;
        this.metadataRegistry = metadataRegistry;
    }
    
    @Override
    public String name() {
        return cfg.name();
    }

    public String baseUrl() {
        return cfg.baseUrl();
    }

    public String authorizationEndpoint() {
        return metadataRegistry.getAuthorizationEndpoint(cfg.baseUrl()).toString();
    }
    
    public String tokenEndpoint() {
        return metadataRegistry.getTokenEndpoint(cfg.baseUrl()).toString();
    }
    
    public String clientId() {
        return cfg.clientId();
    }

    public String clientSecret() {
        return cfg.clientSecret();
    }

    public String[] scopes() {
        return cfg.scopes();
    }

    public String[] additionalAuthorizationParameters() {
        return cfg.additionalAuthorizationParameters();
    }
}
