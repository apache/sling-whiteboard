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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Provider;
import java.security.Security;

public class Activator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(BundleActivator.class);

    public void start(BundleContext context) throws Exception {
        logger.info("Activating Apache Sling SAML2 SP Bundle. And Initializing JCE, Java Cryptographic Extension");
        JavaCryptoValidationInitializer jcvi = new JavaCryptoValidationInitializer();

        try {
            jcvi.init();
            for (Provider jceProvider : Security.getProviders()) {
                logger.info(jceProvider.getInfo());
            }
        } catch (InitializationException e) {
            throw new Error("Java Cryptographic Extension could not initialize. " +
                    "This happens when JCE implementation is incomplete, and not meeting OpenSAML standards.", e);
        }

/*
TODO: Check whether SLing Dev's have advice about this classloading / initialization
The suggestion in this post
https://medium.com/@dehami.deshan/commencing-migration-towards-the-checked-flag-opensaml-3-cc62d3faa3b0
fixes a issue similar to what is discussed below
https://shibboleth.1660669.n2.nabble.com/Null-returned-by-XMLObjectProviderRegistrySupport-getBuilderFactory-td7643173.html
*/
        Thread thread = Thread.currentThread();
        ClassLoader loader = thread.getContextClassLoader();
        thread.setContextClassLoader(InitializationService.class.getClassLoader());
        try {
            InitializationService.initialize();
        } finally {
            thread.setContextClassLoader(loader);
        }
    }

    public void stop(BundleContext context) throws Exception {
        // do something at bundle start
    }
}