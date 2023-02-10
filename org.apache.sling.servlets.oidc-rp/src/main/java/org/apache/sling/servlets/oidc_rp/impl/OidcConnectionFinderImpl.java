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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.oidc_rp.OidcConnectionFinder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OidcConnectionFinderImpl implements OidcConnectionFinder, OidcConnectionPersister {

    private static final String PROPERTY_NAME_EXPIRES_AT = "expiresAt";
    private static final String PROPERTY_NAME_ACCESS_TOKEN = "access_token";
    private static final String PROPERTY_NAME_REFRESH_TOKEN = "refresh_token";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OidcConnection connection;

    @Activate
    public OidcConnectionFinderImpl(@Reference OidcConnection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<String> getOidcToken(ResourceResolver resolver) {
        try {
            User user = resolver.adaptTo(User.class);

            Value[] expiresAt = user.getProperty(propertyPath(PROPERTY_NAME_EXPIRES_AT));
            if ( expiresAt != null  && expiresAt.length == 1 && expiresAt[0].getType() == PropertyType.DATE ) {
                Calendar expiresCal = expiresAt[0].getDate();
                if ( expiresCal.before(Calendar.getInstance())) {
                    logger.info("Token for {} expired at {}, removing", connection.name(), expiresCal);
                    user.removeProperty(nodePath()); // unsure if this will work ...
                    return Optional.empty();
                }
            }
            
            // TODO - how to handle scenario when access_token is null but refresh_token exists?

            Value[] tokenValue = user.getProperty(propertyPath(PROPERTY_NAME_ACCESS_TOKEN));
            if ( tokenValue == null )
                return Optional.empty();

            if ( tokenValue.length != 1)
                throw new RuntimeException("Unexpected value count for token property : " + tokenValue.length);

            return Optional.of(tokenValue[0].getString());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void persistToken(ResourceResolver resolver, String tokenValue, String refreshToken, ZonedDateTime expiry) {
        try {
            User currentUser = resolver.adaptTo(User.class);
            Session session = resolver.adaptTo(Session.class);
            currentUser.setProperty(propertyPath(PROPERTY_NAME_ACCESS_TOKEN), session.getValueFactory().createValue(tokenValue));
            if ( expiry != null ) {
                Calendar cal = GregorianCalendar.from(expiry);
                currentUser.setProperty(propertyPath(PROPERTY_NAME_EXPIRES_AT), session.getValueFactory().createValue(cal));
            } else
                currentUser.removeProperty(propertyPath(PROPERTY_NAME_EXPIRES_AT));
            
            if ( refreshToken != null )
                currentUser.setProperty(propertyPath(PROPERTY_NAME_REFRESH_TOKEN), session.getValueFactory().createValue(refreshToken));
            else
                currentUser.removeProperty(propertyPath(PROPERTY_NAME_REFRESH_TOKEN));
            
            session.save();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URI getOidcEntryPointUri(SlingHttpServletRequest request, String redirectPath) {
        StringBuilder uri = new StringBuilder();
        uri.append(request.getScheme()).append("://").append(request.getServerName()).append(":").append(request.getServerPort()).append(OidcEntryPointServlet.PATH);
        if ( redirectPath != null )
            uri.append("?redirect=").append(URLEncoder.encode(redirectPath, StandardCharsets.UTF_8));

        return URI.create(uri.toString());
    }

    private String propertyPath(String propertyName) {
        return nodePath() + "/" + propertyName;
    }

    private String nodePath() {
        return "oidc-tokens/" + connection.name();
    }

}
