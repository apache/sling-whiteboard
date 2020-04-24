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

import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.x509.BasicX509Credential;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class KeyPairCredentials {

    public static BasicX509Credential getCredential(
            final String jksPath,
            final String jksPassword,
            final String certAlias,
            final String keysPassword) {
        FileInputStream fis = null;
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            fis = new FileInputStream(jksPath);
            keyStore.load(new FileInputStream(jksPath), jksPassword.toCharArray());
            Key key = keyStore.getKey(certAlias, keysPassword.toCharArray());
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(certAlias);
            PublicKey publicKey = cert.getPublicKey();
            KeyPair keyPair = new KeyPair(publicKey, (PrivateKey) key);
            return CredentialSupport.getSimpleCredential(cert,keyPair.getPrivate() );
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
        } catch (UnrecoverableKeyException e) {
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