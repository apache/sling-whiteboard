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

package org.apache.sling.remotecontent.samples.graphql;

import org.apache.sling.graphql.api.ScalarConversionException;
import org.apache.sling.graphql.api.SlingScalarConverter;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;

/** Object Scalar, used to handle any data type */
@Component(service = SlingScalarConverter.class, property = { "name=Object" })
public class ObjectScalar implements SlingScalarConverter<Object, Object> {

    @Override
    public @Nullable Object parseValue(@Nullable Object input) throws ScalarConversionException {
        // TODO validate and convert the input value - if we want to keep hiding the graphql-java APIs
        // we'll need the GraphQL Core to convert the input to the appropriate class
        return input;
    }

    @Override
    public @Nullable Object serialize(@Nullable Object value) throws ScalarConversionException {
        // TODO validate and convert the value - if we want to keep hiding the graphql-java APIs
        // we'll need the GraphQL Core to convert the input to the appropriate class
        return value;
    }

}