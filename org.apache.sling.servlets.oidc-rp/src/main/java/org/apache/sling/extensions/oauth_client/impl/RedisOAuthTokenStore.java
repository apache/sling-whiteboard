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
package org.apache.sling.extensions.oauth_client.impl;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.extensions.oauth_client.ClientConnection;
import org.apache.sling.extensions.oauth_client.OAuthException;
import org.apache.sling.extensions.oauth_client.OAuthToken;
import org.apache.sling.extensions.oauth_client.OAuthTokenStore;
import org.apache.sling.extensions.oauth_client.OAuthTokens;
import org.apache.sling.extensions.oauth_client.TokenState;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Component(configurationPolicy = REQUIRE)
@Designate(ocd = RedisOAuthTokenStore.Config.class)
public class RedisOAuthTokenStore implements OAuthTokenStore {
    
    @ObjectClassDefinition(name = "Redis OAuth Token Store")
    static @interface Config {
        @AttributeDefinition(name = "Redis URL")
        String redisUrl();
    }

    private static final String KEY_PREFIX = "sling.oauth.tokens";
    
    private static final String KEY_SEGMENT_ACCESS_TOKEN = "access_token";
    private static final String KEY_SEGMENT_REFRESH_TOKEN = "refresh_token";
    
    private final JedisPool pool;
    
    @Activate
    public RedisOAuthTokenStore(Config cfg) {
        pool = new JedisPool(cfg.redisUrl());
    }
    
    public void deactivate() {
        pool.close();
    }

    @Override
    public OAuthToken getAccessToken(ClientConnection connection, ResourceResolver resolver) throws OAuthException {
        String userId = resolver.getUserID();
        
        try ( Jedis jedis = pool.getResource()) {
            String accessToken = jedis.get(keyFor(userId, connection, KEY_SEGMENT_ACCESS_TOKEN));
            if ( accessToken != null ) {
                return new OAuthToken(TokenState.VALID , accessToken);
            }
            
            String refreshToken = jedis.get(keyFor(userId, connection, KEY_SEGMENT_REFRESH_TOKEN));
            if ( refreshToken != null ) {
                return new OAuthToken(TokenState.EXPIRED, null);
            }
            
            return new OAuthToken(TokenState.MISSING, null);
        }
    }

    @Override
    public OAuthToken getRefreshToken(ClientConnection connection, ResourceResolver resolver) throws OAuthException {
        String userId = resolver.getUserID();
        
        try (Jedis jedis = pool.getResource()) {
            String refreshToken = jedis.get(keyFor(userId, connection, KEY_SEGMENT_REFRESH_TOKEN));
            if (refreshToken != null) {
                return new OAuthToken(TokenState.VALID, refreshToken);
            }

            return new OAuthToken(TokenState.MISSING, null);
        }
    }

    @Override
    public void persistTokens(ClientConnection connection, ResourceResolver resolver, OAuthTokens tokens)
            throws OAuthException {
        String userId = resolver.getUserID();

        try (Jedis jedis = pool.getResource()) {
            setWithExpiry(jedis, keyFor(userId, connection, KEY_SEGMENT_ACCESS_TOKEN), tokens.accessToken(), tokens.expiresAt() );
            if ( tokens.refreshToken() != null )
                jedis.set(keyFor(userId, connection, KEY_SEGMENT_REFRESH_TOKEN), tokens.refreshToken());
        }
    }
    
    private void setWithExpiry(Jedis jedis, String key, String value, long expiry) {
        jedis.set(key, value);
        jedis.expire(key, expiry);
    }
    
    private String keyFor(String principal, ClientConnection connection, String tokenType) {
        return KEY_PREFIX + "." + principal + "." + connection.name() + "." + tokenType;
    }
}
