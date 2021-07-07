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

package org.apache.sling.graphql.schema.aggregator.api;

import java.io.IOException;
import java.io.Writer;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface SchemaAggregator {
    /** Aggregate the schemas supplied by partial schame providers which match the exact names
     *  or patterns supplied.
     *
     *  @param target where to write the output
     *
     *  @param providerNamesOrRegexp a value that starts and ends with a slash is used a a regular
     *      expression to match provider names (after removing the starting and ending slash), other
     *      values are used as exact provider names, which are then required.
     *
     *  @throws IOException if an exact provider name is not found
     */
    void aggregate(Writer target, String ... providerNamesOrRegexp) throws IOException;
}
