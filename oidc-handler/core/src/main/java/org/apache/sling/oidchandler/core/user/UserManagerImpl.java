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
package org.apache.sling.oidchandler.core.user;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.sling.jcr.api.SlingRepository;

public class UserManagerImpl {

    private final Logger logger = LoggerFactory.getLogger(UserManagerImpl.class);
    private Session adminSession;

    private static SlingRepository getSlingRepository() {
        BundleContext bundleContext = FrameworkUtil.getBundle(SlingRepository.class).getBundleContext();
        return (SlingRepository)bundleContext.getService(bundleContext
                .getServiceReference(SlingRepository.class.getName()));
    }

    public boolean createUser(String name, String password) throws RepositoryException {
        logger.info("creating user with id '{}'", name);

        // usual entry point into the Jackrabbit API
        Session session = getSession();
        UserManager userManager = getUserManager(session);
        String usersPath = "/" + name;
        User user = userManager.createUser(name, password);
        user.setProperty("homeFolder", session.getValueFactory().createValue(usersPath));
        session.save();

        return true;
    }

    public User getUser(final Credentials credentials) {
        if (credentials instanceof SimpleCredentials) {
            final SimpleCredentials simpleCredentials = (SimpleCredentials) credentials;
            final String userId = simpleCredentials.getUserID();
            return getUser(userId);
        }
        return null;
    }

    protected User getUser(final String userId) {
        logger.info("getting user with id '{}'", userId);
        try {
            Session session = getSession();
            UserManager userManager = getUserManager(session);
            Authorizable authorizable = userManager.getAuthorizable(userId);
            if (authorizable != null) {
                if (authorizable instanceof User) {
                    User user = (User) authorizable;
                    logger.debug("user for id '{}' found", userId);
                    return user;
                } else {
                    logger.debug("found authorizable with id '{}' is not an user", authorizable.getID());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private synchronized Session getSession() throws RepositoryException {
        if (adminSession == null || !adminSession.isLive()) {
            adminSession = getSlingRepository().loginAdministrative(null);
        }
        return adminSession;
    }

    private UserManager getUserManager(Session session) throws RepositoryException {
        if (session instanceof JackrabbitSession) {
            JackrabbitSession jackrabbitSession = (JackrabbitSession) session;
            return jackrabbitSession.getUserManager();
        } else {
            logger.error("Cannot get UserManager from session: not a Jackrabbit session");
            return null;
        }
    }

}
