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

import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.xmlsec.config.impl.JavaCryptoValidationInitializer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.*;

public class Activator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(BundleActivator.class);

    public void start(BundleContext context) throws Exception {
        // Classloading
        BundleWiring bundleWiring = context.getBundle().adapt(BundleWiring.class);
        ClassLoader loader = bundleWiring.getClassLoader();
        Thread thread = Thread.currentThread();
        thread.setContextClassLoader(InitializationService.class.getClassLoader());
        try {
            initializeOpenSaml();
        } catch (InitializationException e) {
            throw new SAML2RuntimeException("Java Cryptographic Extension could not initialize. " +
                    "This happens when JCE implementation is incomplete, and not meeting OpenSAML standards.", e);
        } finally {
            thread.setContextClassLoader(loader);
        }
        // TODO add the Jaas config related to SAML2 so it's one less thing to configure
    }

    public void stop(BundleContext context) throws Exception {
        // TODO remove the Jaas config related to SAML2 so authentication in general isn't broken when bundle is deactivated
    }

    public static void initializeOpenSaml() throws InitializationException{
        JavaCryptoValidationInitializer jcvi = new JavaCryptoValidationInitializer();
        jcvi.init();
        InitializationService.initialize();
        logger.info("Activating Apache Sling SAML2 SP Bundle. And Initializing JCE, Java Cryptographic Extension");
        for (Provider jceProvider : Security.getProviders()) {
            logger.info(jceProvider.getInfo());
        }
    }
}