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

package org.apache.sling.jsonstore.internal.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

/** A Command that can be run against our store */
@ConsumerType
public interface Command {
    /** Service property to define the command namespace */
    public static final String SERVICE_PROP_NAMESPACE = "cmd.namespace";
    /** Service property to define the command name */
    public static final String SERVICE_PROP_NAME = "cmd.name";

    /** Get info on the command */
    @NotNull JsonNode getInfo();

    /** Execute the command */
    @NotNull JsonNode execute(ResourceResolver resolver, JsonNode input) throws IOException;
}