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
package org.apache.sling.remote.resourceprovider.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Apache Sling Remote Resource Provider Factory",
        description = "The Apache Sling Remote Resource Provider Factory is responsible for 1:1 mappings between a RemoteStorageProvider " +
                "and a Resource Provider."
)
public @interface RemoteResourceProviderFactoryConfiguration {

    @AttributeDefinition(
            name = "Resource Tree Cache Size",
            description = "The number of resources to be stored in memory by each registered Resource Provider. Values under 100 disable " +
                    "the cache."
    )
    int cacheSize() default 10000;

    @AttributeDefinition(
            name = "Access Cache Expiration",
            description = "The number of minutes since the last access operation of a cached entry after which the entry will be removed " +
                    "from the cache. 0 or a negative value disable the cache."
    )
    int lastAccessedExpirationTime() default 5;

}
