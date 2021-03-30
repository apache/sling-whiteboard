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

package org.apache.sling.auth.saml2;

public interface SAML2ConfigService {
    String[] getSaml2Path();
    String getEntityID();
    String getAcsPath();
    String getACSURL();
    String getSaml2userHome();
    String getSaml2userIDAttr();
    String getSaml2groupMembershipAttr();
    String getSaml2SessionAttr();
    String getSaml2IDPDestination();
    boolean getSaml2SPEnabled();
    boolean getSaml2SPEncryptAndSign();
    String[] getSyncAttrs();
    String getJksFileLocation();
    String getJksStorePassword();
    String getSpKeysAlias();
    String getIdpCertAlias();
    String getSpKeysPassword();
    String getSaml2LogoutURL();
}
