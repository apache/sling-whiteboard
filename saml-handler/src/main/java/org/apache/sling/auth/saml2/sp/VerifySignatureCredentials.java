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
 *
 */

package org.apache.sling.auth.saml2.sp;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.Criterion;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.credential.impl.KeyStoreCredentialResolver;
import org.opensaml.security.x509.BasicX509Credential;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * originally created by Privat on 13/05/14.
 */
public class SPCredentials {
    private static final String KEY_STORE_PASSWORD = "password";
    private static final String KEY_STORE_ENTRY_PASSWORD = "password";
    private static final String KEY_STORE_PATH = "/SPKeystore.jks";
    private static final String KEY_ENTRY_ID = "SPKey";

    private static final Credential credential;

    static {
        try {
            KeyStore keystore = readKeystoreFromFile(KEY_STORE_PATH, KEY_STORE_PASSWORD);
            Map<String, String> passwordMap = new HashMap<String, String>();
            passwordMap.put(KEY_ENTRY_ID, KEY_STORE_ENTRY_PASSWORD);
            KeyStoreCredentialResolver resolver = new KeyStoreCredentialResolver(keystore, passwordMap);

            Criterion criterion = new EntityIdCriterion(KEY_ENTRY_ID);
            CriteriaSet criteriaSet = new CriteriaSet();
            criteriaSet.add(criterion);

            credential = resolver.resolveSingle(criteriaSet);

        } catch (ResolverException e) {
            throw new RuntimeException("Something went wrong reading credentials", e);
        }
    }

    private static KeyStore readKeystoreFromFile(String pathToKeyStore, String keyStorePassword) {
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream inputStream = SPCredentials.class.getResourceAsStream(pathToKeyStore);
            keystore.load(inputStream, keyStorePassword.toCharArray());
            inputStream.close();
            return keystore;
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong reading keystore", e);
        }
    }

    public static Credential getCredential() {
        return credential;
    }

    public static Credential getCredential(
            final String jksPath,
            final String jksPassword,
            final String certAlias) {
        FileInputStream fis = null;
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            fis = new FileInputStream(jksPath);
            keyStore.load(new FileInputStream(jksPath), jksPassword.toCharArray());
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(certAlias);
            BasicX509Credential x509Credential = new BasicX509Credential(cert);
            return x509Credential;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (java.security.KeyStoreException e) {
            throw new RuntimeException(e);
        }  catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
