
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

package org.apache.sling.scripting.gql.engine;

import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/** Provides a Resource-specific GraphQL Schema, as text */
@Component(
    service = GraphQLSchemaProvider.class,
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Scripting GraphQL SchemaProvider",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)
public class GraphQLSchemaProvider {

    public String getSchema(Resource r) {
        return
            "type Query { currentResource : SlingResource }\n"
            + "type SlingResource { path: String resourceType: String }\n"
        ;
    }

}
