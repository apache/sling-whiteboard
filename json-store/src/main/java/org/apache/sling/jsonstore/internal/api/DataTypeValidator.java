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
import org.osgi.annotation.versioning.ConsumerType;

/** Interface to validate our various data types: schema, elements, content, ...
 *  Several such services are meant to be provided, at least one
 *  for each data type.
 */
@ConsumerType
public interface DataTypeValidator {
    public static class ValidatorException extends IOException {
        public ValidatorException(String reason) {
            super(reason);
        }
        public ValidatorException(String reason, Throwable cause) {
            super(reason, cause);
        }
    }

    /** Validate a specific data type
     *  @return true if this validator applies to the arguments. True does not mean the input
     *      is valid, only that that validator applies to it.
     *  @throws ValidatorException if there's a validation error
     */
    boolean validate(ResourceResolver resolver, JsonNode json, String site, String dataType) throws ValidatorException;
}