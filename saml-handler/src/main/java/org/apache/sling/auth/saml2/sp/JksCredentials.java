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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

public abstract class JksCredentials {

    public static KeyStore getKeyStore(String filePathToJKS, char[] jksPassword) {
        // Try-with-Resources closes file input stream automatically
        try (InputStream fis = new FileInputStream(filePathToJKS)){
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(fis, jksPassword);
            return keyStore;
        } catch (FileNotFoundException e) {
            throw new SAML2RuntimeException(e);
        } catch (IOException e) {
            throw new SAML2RuntimeException(e);
        } catch (java.security.KeyStoreException e) {
            throw new SAML2RuntimeException(e);
        }  catch (NoSuchAlgorithmException e) {
            throw new SAML2RuntimeException(e);
        } catch (CertificateException e) {
            throw new SAML2RuntimeException(e);
        }
    }
}