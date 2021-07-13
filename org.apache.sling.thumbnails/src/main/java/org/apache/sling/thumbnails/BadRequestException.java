/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.thumbnails;

import java.util.stream.Collectors;

import org.apache.sling.api.resource.ValueMap;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Exception to indicate that the request provided is invalid
 */
@ProviderType
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Exception cause) {
        super(message, cause);
    }

    public BadRequestException(String message, ValueMap properties) {
        super(String.format(message, properties.entrySet().stream().map(en -> en.getKey() + "=" + en.getValue())
                .collect(Collectors.joining("\n"))));
    }

    public BadRequestException(String message, ValueMap properties, Exception cause) {
        super(String.format(message, properties.entrySet().stream().map(en -> en.getKey() + "=" + en.getValue())
                .collect(Collectors.joining("\n"))), cause);
    }
}