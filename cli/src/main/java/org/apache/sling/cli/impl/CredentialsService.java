/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.cli.impl;

import java.util.Optional;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(service = CredentialsService.class)
public class CredentialsService {

    private static final String USER_SYS_PROP = "asf.username";
    private static final String PASSWORD_SYS_PROP = "asf.password";
    private static final String USER_ENV_PROP = "ASF_USERNAME";
    private static final String PASSWORD_ENV_PROP = "ASF_PASSWORD";

    private volatile Credentials credentials;

    @Activate
    private void activate() {
        Optional<String> username =
                Optional.ofNullable(System.getProperty(USER_SYS_PROP)).or(() -> Optional.ofNullable(System.getenv(USER_ENV_PROP)));
        Optional<String> password =
                Optional.ofNullable(System.getProperty(PASSWORD_SYS_PROP)).or(() -> Optional.ofNullable(System.getenv(PASSWORD_ENV_PROP)));
        credentials = new Credentials(
                username.orElseThrow(() -> new IllegalStateException(
                        String.format("Cannot detect user information after looking for %s system property and %s environment variable.",
                                USER_SYS_PROP, USER_ENV_PROP))),
                password.orElseThrow(() -> new IllegalStateException(
                        String.format("Cannot detect password after looking for %s system property and %s environment variable.",
                                PASSWORD_SYS_PROP, PASSWORD_ENV_PROP)))
        );
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
