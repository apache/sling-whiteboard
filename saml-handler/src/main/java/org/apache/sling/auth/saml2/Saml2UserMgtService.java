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

import org.apache.jackrabbit.api.security.user.User;

public interface Saml2UserMgtService {

    /**
     * Call setUp before using any other Saml2UserMgtService method
     */
    boolean setUp();

    /**
     * getOrCreateSamlUser(Saml2User user) will be called if userHome is not configured
     * @param user
     * @return
     */
    User getOrCreateSamlUser(Saml2User user);
    
    /**
     * getOrCreateSamlUser(Saml2User user) will be called if userHome is configured
     * @param user
     * @return
     */
    User getOrCreateSamlUser(Saml2User user, String userHome);
    
    /**
     * Users group membership will be updated based on the groups contained in the 
     * configured element of the SAML Assertion
     */
    boolean updateGroupMembership(Saml2User user);

    /**
     * Users properties will be updated based on user properties contained in the 
     * configured properties of the SAML Assertion
     */
    boolean updateUserProperties(Saml2User user);
    
    /**
     * Call cleanUp after using Saml2UserMgtService methods
     */
    void cleanUp();
}
