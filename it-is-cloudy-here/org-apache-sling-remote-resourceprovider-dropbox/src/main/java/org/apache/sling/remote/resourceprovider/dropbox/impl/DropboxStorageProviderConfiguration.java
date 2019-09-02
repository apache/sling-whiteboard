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

package org.apache.sling.remote.resourceprovider.dropbox.impl;

import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Dropbox Remote Storage Provider Configuration",
        description = "The Dropbox Remote Storage Provider allows mapping Dropbox provided resources in the Sling Resource tree."
)
@interface DropboxStorageProviderConfiguration {
    @AttributeDefinition(
            name = "App key",
            description = "The application key defined at https://www.dropbox.com/developers/apps/info/<appKey>"
    )
    String appKey() default "";

    @AttributeDefinition(
            name = "App secret",
            description = "The application secret defined at https://www.dropbox.com/developers/apps/info/<appKey>"
    )
    String appSecret() default "";

    @AttributeDefinition(
            name = "Access token",
            description = "An access token to skip OAuth."
    )
    String accessToken() default "";

    @AttributeDefinition(
            name = "Resource Provider Root",
            description = "The root of the Resource Provider where Dropbox resources will be mapped into the Sling Resource tree."
    )
    String resource_provider_root();

    @AttributeDefinition(
            name = "Remote Storage Provider Root",
            description = "The Dropbox root folder from which directory and files will be used to build the Sling resource tree " +
                    "provided at the path indicated by resource.provider.root. \"/\" corresponds to Dropbox integration's root folder."
    )
    String remote_storage_provider_root() default "/";

    @AttributeDefinition(
            description = "Select 'No' only if using a pre-configured access token.",
            options = {
                    @Option(value = ResourceProvider.AUTHENTICATE_LAZY, label = "Lazy"),
                    @Option(value = ResourceProvider.AUTHENTICATE_NO, label = "No"),
            }
    )
    String resource_provider_authenticate() default ResourceProvider.AUTHENTICATE_NO;
}
