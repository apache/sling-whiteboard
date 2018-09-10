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
package org.apache.sling.oidchandler.core.endpoints;

import com.nimbusds.oauth2.sdk.ParseException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.oidchandler.core.configuration.OIDCConfigServlet;
import org.apache.sling.oidchandler.core.exception.AuthenticationError;
import org.apache.sling.oidchandler.core.handlers.Handler;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Component(service = Servlet.class, property = {"sling.servlet.methods={GET, POST}","sling.servlet.paths=/auth/login"},immediate = true)
public class LoginEndpoint extends SlingAllMethodsServlet {
    private final Logger logger = LoggerFactory.getLogger(LoginEndpoint.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

        Boolean enabled = OIDCConfigServlet.getOIDCEnabled();

        if (enabled) {
            try {

                URI uri = Handler.getHandler().createAuthenticationRequest();

                if (uri != null) {
                    response.setStatus(302);
                    response.setHeader("Location",uri.toString());

                } else {
                    logger.info("Error occured while creating the Authentication request.");
                    AuthenticationError.sendAuthenticationError(response);
                }

            } catch (URISyntaxException e) {
                logger.info("Error occured while creating the Authentication request.");
            } catch (ParseException e) {
                logger.info("Error occured while creating the Authentication request.");
            }
        } else {
            logger.info("OpenID Connect handler is not enabled in OSGI configurations.");
            AuthenticationError.sendAuthenticationError(response);
        }

    }
}
