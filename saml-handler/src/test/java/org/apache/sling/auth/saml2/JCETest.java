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

import org.junit.Test;
import org.opensaml.core.config.InitializationException;
import org.opensaml.xmlsec.config.impl.JavaCryptoValidationInitializer;
import java.security.Provider;
import java.security.Security;

public class JCETest {

    @Test
    public void jceValidationTest (){
        JavaCryptoValidationInitializer jcvi = new JavaCryptoValidationInitializer();
        try {
            jcvi.init();
            for (Provider jceProvider : Security.getProviders()) {
                System.out.print(jceProvider.getInfo());
            }
        } catch (InitializationException e) {
            throw new Error("Java Cryptographic Extension could not initialize. " +
                "This happens when JCE implementation is incomplete, and not meeting OpenSAML standards.", e);
        }
    }
}
