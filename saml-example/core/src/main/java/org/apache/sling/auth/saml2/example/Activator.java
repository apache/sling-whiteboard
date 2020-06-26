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

package org.apache.sling.auth.saml2.example;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;


public class Activator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(BundleActivator.class);

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        logger.info("SAML2 Example Bundle");
        createExampleJks();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

    }

    void createExampleJks(){
        KeyStore ks = null;
        try {
            File file = new File("./sling/keys/exampleSaml2.jks");
            char[] password = "password".toCharArray();
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            if (!file.exists()) {
                FileOutputStream fos = new FileOutputStream(file);
                ks.load(null, password);
                ks.store(fos, password);
                fos.close();
                logger.info("Example JKS created");
            } else {
                logger.info("Example JKS exists");
            }
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            logger.error("Error encountered creating JKS", e);
        }
    }
}
