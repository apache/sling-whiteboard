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
package org.apache.sling.servlets.oidc_rp.impl;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.sling.servlets.oidc_rp.OidcException;
import org.osgi.service.component.annotations.Component;

import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

/**
 * A registry for provider metadata
 * 
 * <p>Encapsulates the logic for retrieving the {@link #OIDCProviderMetadata} for a given 
 * connection.</p>
 * 
 * <p>Maintains a best-effort, unbounded, cache for the metadata, on the assumption that
 * there will be a small number of OIDC connections configured.</p>
 */
@Component(service = OidcProviderMetadataRegistry.class)
public class OidcProviderMetadataRegistry {
    private final ConcurrentMap<String, OIDCProviderMetadata> cache = new ConcurrentHashMap<>();

    public OIDCProviderMetadata getProviderMetadata(String base) {
        return cache.computeIfAbsent(base, s -> {
            try {
                return OIDCProviderMetadata.resolve(new Issuer(s));
            } catch (GeneralException | IOException e) {
                throw new OidcException(e);
            }
        });
    }
}