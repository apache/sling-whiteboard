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
package org.apache.sling.servlets.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.sling.servlets.json.dynamicrequest.DuplicateHandlersServlet;
import org.apache.sling.servlets.json.dynamicrequest.InvalidArgumentsServlet;
import org.apache.sling.servlets.json.dynamicrequest.RequestMappingException;
import org.apache.sling.servlets.json.dynamicrequest.SampleDynamicRequestServlet;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(SlingContextExtension.class)
class DynamicRequestServletTest {

    private final SampleDynamicRequestServlet servlet = new SampleDynamicRequestServlet();

    private final SlingContext context = new SlingContext();

    @Test
    void testSimple() throws ServletException, IOException {
        context.request().setServletPath("/simple");
        context.request().setMethod("GET");
        servlet.service(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("application/json;charset=UTF-8", context.response().getContentType());
        assertEquals("{\"Hello\":\"World\"}", context.response().getOutputAsString());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "/simple/bob,200",
            "/simple/bob/sal,200",
            "/simple/bob.png,200",
            "/simple2/glob,405"
    })
    void testGlob(String path, int status) throws ServletException, IOException {
        context.request().setServletPath(path);
        context.request().setMethod("GET");
        servlet.service(context.request(), context.response());
        assertEquals(status, context.response().getStatus());
    }

    @Test
    void testOrdering() throws ServletException, IOException {
        context.request().setServletPath("/simple/glob");
        context.request().setMethod("GET");
        servlet.service(context.request(), context.response());
        assertEquals(200, context.response().getStatus());
        assertEquals("{\"Hello\":\"World2\"}", context.response().getOutputAsString());
    }

    @Test
    void testNoResponse() throws ServletException, IOException {
        context.request().setServletPath("/no-response");
        context.request().setMethod("POST");
        servlet.service(context.request(), context.response());
        assertEquals(202, context.response().getStatus());
        assertEquals("", context.response().getOutputAsString());
    }

    @Test
    void supportsParameters() throws ServletException, IOException {
        context.request().setServletPath("/with-param");
        context.request().setMethod("GET");
        context.request().addRequestParameter("name", "Sling");
        servlet.service(context.request(), context.response());
        assertEquals(200, context.response().getStatus());
        assertEquals("{\"Hello\":\"Sling\"}", context.response().getOutputAsString());
    }

    @Test
    void supportsMissingParams() throws ServletException, IOException {
        context.request().setServletPath("/with-param");
        context.request().setMethod("GET");
        servlet.service(context.request(), context.response());
        assertEquals(200, context.response().getStatus());
        assertEquals("{\"Hello\":null}", context.response().getOutputAsString());
    }

    @Test
    void handlesThrownExceptions() throws ServletException, IOException {
        context.request().setServletPath("/npe");
        context.request().setMethod("GET");
        servlet.service(context.request(), context.response());
        assertEquals(500, context.response().getStatus());
        assertEquals("application/problem+json;charset=UTF-8", context.response().getContentType());
        assertEquals(
                "{\"title\":\"Internal Server Error\",\"status\":500,\"detail\":\"java.lang.NullPointerException\"}",
                context.response().getOutputAsString());
    }

    @Test
    void wontConstructWithDuplicateHandlers() {
        assertThrows(RequestMappingException.class, () -> new DuplicateHandlersServlet());
    }

    @Test
    void wontConstructWithInvalidArguments() {
        assertThrows(Exception.class, () -> new InvalidArgumentsServlet());
    }
}
