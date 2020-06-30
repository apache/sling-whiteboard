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


import org.apache.sling.auth.saml2.SAML2RuntimeException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import java.security.*;
import java.security.cert.X509Certificate;

/**
 * This is used when SAML messages are signed by the Identity Provider.
 */
public class VerifySignatureCredentials extends JksCredentials {

    private VerifySignatureCredentials(){
        super();
    }

    /**
     * The public x509 credential as provided by this method is used to verify the signature of incoming SAML Requests.
     *
     * @param jksPath The path to the JKS holding the Certification
     * @param jksPassword Password of the Java KeyStore
     * @param certAlias The Alias of the public key credential used to create the X509
     * @return
     */

    public static Credential getCredential(
            final String jksPath,
            final char[] jksPassword,
            final String certAlias) {

        try {
            KeyStore keyStore = getKeyStore(jksPath, jksPassword);
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(certAlias);
            BasicX509Credential x509Credential = new BasicX509Credential(cert);
            return x509Credential;
        } catch (java.security.KeyStoreException e) {
            throw new SAML2RuntimeException(e);
        }
    }

}
