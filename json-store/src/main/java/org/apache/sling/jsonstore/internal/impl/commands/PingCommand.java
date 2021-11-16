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

package org.apache.sling.jsonstore.internal.impl.commands;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.sling.jsonstore.internal.api.Command;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

@Component(
    service=Command.class,
    property = {
        Command.SERVICE_PROP_NAMESPACE + "=cmd",
        Command.SERVICE_PROP_NAME + "=ping",
    }
)
public class PingCommand implements Command {

    @Override
    public @NotNull JsonNode getInfo() {
        final ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("command", getClass().getName());
        result.put("description", "returns its input, used to 'ping' the service");
        return result;
    }

    @Override
    public @NotNull JsonNode execute(JsonNode input) throws IOException {
        final ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("command", getClass().getName());
        result.replace("input", input);
        return result;
    }

}