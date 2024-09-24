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

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.extensions.oauth_client.OAuthException;
import org.apache.sling.extensions.oauth_client.OAuthToken;
import org.apache.sling.extensions.oauth_client.OAuthTokenStore;
import org.apache.sling.extensions.oauth_client.OAuthTokens;
import org.apache.sling.extensions.oauth_client.ClientConnection;
import org.apache.sling.extensions.oauth_client.TokenState;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// a config class is intentionally not defined, but a config is required to select an implementation
@Component(configurationPolicy = REQUIRE)
public class JcrUserHomeOAuthTokenStore implements OAuthTokenStore {

    private static final String PROPERTY_NAME_EXPIRES_AT = "expires_at";
    private static final String PROPERTY_NAME_ACCESS_TOKEN = "access_token";
    private static final String PROPERTY_NAME_REFRESH_TOKEN = "refresh_token";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public OAuthToken getAccessToken(ClientConnection connection, ResourceResolver resolver) {
        try {
            User user = resolver.adaptTo(User.class);

            Value[] expiresAt = user.getProperty(propertyPath(connection, PROPERTY_NAME_EXPIRES_AT));
            if ( expiresAt != null  && expiresAt.length == 1 && expiresAt[0].getType() == PropertyType.DATE ) {
                Calendar expiresCal = expiresAt[0].getDate();
                if ( expiresCal.before(Calendar.getInstance())) {
                    logger.info("Token for {} expired at {}, marking as expired", connection.name(), expiresCal);

                    // refresh token is present, mark as expired
                    return new OAuthToken(TokenState.EXPIRED, null);
                }
            }

            return getToken(connection, user, PROPERTY_NAME_ACCESS_TOKEN);
        } catch (RepositoryException e) {
            throw new OAuthException(e);
        }
    }

    private OAuthToken getToken(ClientConnection connection, User user, String propertyName) throws RepositoryException {

        Value[] tokenValue = user.getProperty(propertyPath(connection, propertyName));
        if ( tokenValue == null )
            return new OAuthToken(TokenState.MISSING, null);

        if ( tokenValue.length != 1)
            throw new OAuthException(String.format("Unexpected value count %d for token property %s" , tokenValue.length, propertyName));

        return  new OAuthToken(TokenState.VALID, tokenValue[0].getString());
    }
    
    @Override
    public OAuthToken getRefreshToken(ClientConnection connection, ResourceResolver resolver) {
        try {
            User user = resolver.adaptTo(User.class);
            
            return getToken(connection, user, PROPERTY_NAME_REFRESH_TOKEN);
        } catch (RepositoryException e) {
            throw new OAuthException(e);
        }
    }
    
    @Override
    public void persistTokens(ClientConnection connection, ResourceResolver resolver, OAuthTokens tokens) {
        try {
            User currentUser = resolver.adaptTo(User.class);
            Session session = resolver.adaptTo(Session.class);

            ZonedDateTime expiry = null;
            long expiresAt = tokens.expiresAt();
            if ( expiresAt > 0 ) {
                expiry = ZonedDateTime.now().plusSeconds(expiresAt);
            }

            String accessToken = tokens.accessToken();
            currentUser.setProperty(propertyPath(connection, PROPERTY_NAME_ACCESS_TOKEN), session.getValueFactory().createValue(accessToken));
            if ( expiry != null ) {
                Calendar cal = GregorianCalendar.from(expiry);
                currentUser.setProperty(propertyPath(connection, PROPERTY_NAME_EXPIRES_AT), session.getValueFactory().createValue(cal));
            } else
                currentUser.removeProperty(propertyPath(connection, PROPERTY_NAME_EXPIRES_AT));

            if ( tokens.refreshToken() != null ) {
                String refreshToken = tokens.refreshToken();
                if ( refreshToken != null )
                    currentUser.setProperty(propertyPath(connection, PROPERTY_NAME_REFRESH_TOKEN), session.getValueFactory().createValue(refreshToken));
                else
                    currentUser.removeProperty(propertyPath(connection, PROPERTY_NAME_REFRESH_TOKEN));
            }

            session.save();
        } catch (RepositoryException e) {
            throw new OAuthException(e);
        }
    }

    private String propertyPath(ClientConnection connection, String propertyName) {
        return nodePath(connection) + "/" + propertyName;
    }

    private String nodePath(ClientConnection connection) {
        return "oauth-tokens/" + connection.name();
    }
}
