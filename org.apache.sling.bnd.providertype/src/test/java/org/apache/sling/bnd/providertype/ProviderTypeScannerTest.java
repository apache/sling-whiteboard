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
package org.apache.sling.bnd.providertype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

class ProviderTypeScannerTest {

    @RegisterExtension
    static BndBuilderExtension bndBuilderExtension = new BndBuilderExtension(new ProviderTypeScanner());

    @Test
    void testBuildWithViolations() throws Exception {
        Builder builder = bndBuilderExtension.builder;
        // add classpath entry with api-info.json
        builder.setClasspath(new File[] { new File("src/test/resources") });
        try (Jar jar = builder.build()) {
            List<String> expectedErrors = Arrays.asList(
                    "Type \"org.apache.sling.bnd.providertype.TypeAImpl\" implements provider type \"org.apache.sling.bnd.providertype.TypeA\". This is not allowed!",
                    "Type \"org.apache.sling.bnd.providertype.TypeBExtension\" extends provider type \"org.apache.sling.bnd.providertype.TypeB\". This is not allowed!");
            assertEquals(expectedErrors, builder.getErrors());
            if (!builder.getWarnings().isEmpty()) {
                fail(String.join("\n", builder.getWarnings()));
            }
        }
    }

    @Test
    void testBuildWithoutProviderTypeMetadata() throws Exception {
        Builder builder = bndBuilderExtension.builder;
        try (Jar jar = builder.build()) {
            if (!builder.getErrors().isEmpty()) {
                fail(String.join("\n", builder.getErrors()));
            }
            List<String> expectedWarnings = Arrays.asList(
                    "Could not find resource \"META-INF/api-info.json\" in the classpath");
            assertEquals(expectedWarnings, builder.getWarnings());
        }
    }

    @Test
    void testBuildWithInvalidProviderTypeMetadata() throws Exception {
        Builder builder = bndBuilderExtension.builder;
        // add classpath entry with api-info.json
        builder.setClasspath(new File[] { new File("src/test/resources2") });
        try (Jar jar = builder.build()) {
            List<String> expectedErrors = Arrays.asList(
                    "Resource \"META-INF/api-info.json\" does not contain a field named \"providerTypes\"");
            assertEquals(expectedErrors, builder.getErrors());
            if (!builder.getWarnings().isEmpty()) {
                fail(String.join("\n", builder.getWarnings()));
            }
        }
    }

    @Test
    void testBuildWithInvalidProviderTypeMetadata2() throws Exception {
        Builder builder = bndBuilderExtension.builder;
        // add classpath entry with api-info.json
        builder.setClasspath(new File[] { new File("src/test/resources3") });
        try (Jar jar = builder.build()) {
            assertEquals(2, builder.getErrors().size());
            assertTrue(builder.getErrors().get(0).startsWith("Exception: java.lang.IllegalStateException: Could not parse JSON from resource"));
            if (!builder.getWarnings().isEmpty()) {
                fail(String.join("\n", builder.getWarnings()));
            }
        }
    }
}
