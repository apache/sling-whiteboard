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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.stream.Stream;

import javax.jcr.PathNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(SlingContextExtension.class)
class JacksonJsonServletTest {

    private final SlingContext context = new SlingContext();

    @ParameterizedTest
    @ValueSource(strings = { "GET", "PATCH", "POST" })
    void testBasicServlet(String method) throws ServletException, IOException {
        String body = "{\"Hello\":\"World\"}";

        context.request().setContent(body.getBytes());
        context.request().setMethod(method);

        TestJacksonJsonServlet testServlet = new TestJacksonJsonServlet();
        testServlet.service(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("application/json;charset=UTF-8", context.response().getContentType());
        assertEquals(body, context.response().getOutputAsString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "PUT", "DELEEETE" })
    void testUnsupported(String method) throws ServletException, IOException {
        context.request().setMethod(method);

        TestJacksonJsonServlet testServlet = new TestJacksonJsonServlet();
        testServlet.service(context.request(), context.response());

        assertEquals(405, context.response().getStatus());
        assertEquals("application/problem+json;charset=UTF-8", context.response().getContentType());
        assertEquals("{\"title\":\"Method Not Allowed\",\"status\":405}", context.response().getOutputAsString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "PATCH", "POST", "PUT", "DELETE" })
    void sendsNotAllowedByDefault(String method) throws ServletException, IOException {

        context.request().setMethod(method);

        JacksonJsonServlet defaultServlet = new JacksonJsonServlet() {
        };
        defaultServlet.service(context.request(), context.response());

        assertEquals(405, context.response().getStatus());
        assertEquals("application/problem+json;charset=UTF-8", context.response().getContentType());
        assertEquals("{\"title\":\"Method Not Allowed\",\"status\":405}", context.response().getOutputAsString());
    }

    @Test
    void canDeserializeObject() throws ServletException, IOException {

        JacksonJsonServlet defaultServlet = new JacksonJsonServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                SamplePojo model = super.readRequestBody(req, SamplePojo.class);
                assertEquals("Sling", model.getTitle());
            }
        };

        context.request().setMethod("POST");
        context.request().setContent("{\"title\":\"Sling\"}".getBytes());
        defaultServlet.service(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
    }

    @Test
    void returns400OnInvalidJsonBody() throws ServletException, IOException {

        JacksonJsonServlet defaultServlet = new JacksonJsonServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                SamplePojo model = super.readRequestBody(req, SamplePojo.class);
                assertEquals("Sling", model.getTitle());
            }
        };

        context.request().setMethod("POST");
        context.request().setContent("{\"title\",\"Sling\"}".getBytes());
        defaultServlet.service(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
        assertEquals("application/problem+json;charset=UTF-8", context.response().getContentType());

        assertEquals(
                "{\"title\":\"Bad Request\",\"status\":400,\"detail\":\"Unable to parse request as JSON: Unexpected character (',' (code 44)): was expecting a colon to separate field name and value\\n at [Source: (BufferedReader); line: 1, column: 10]\"}",
                context.response().getOutputAsString());
    }

    @ParameterizedTest
    @MethodSource("provideExceptions")
    void catchesExceptions(Exception ex, int statusCode) throws ServletException, IOException {
        context.request().setMethod("GET");

        JacksonJsonServlet throwyServlet = new JacksonJsonServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                if (ex instanceof ServletException) {
                    throw (ServletException) ex;
                }
                if (ex instanceof IOException) {
                    throw (IOException) ex;
                }
                fail("Unexpected exception type");
            }
        };
        throwyServlet.service(context.request(), context.response());

        assertEquals(statusCode, context.response().getStatus());
        assertEquals("application/problem+json;charset=UTF-8", context.response().getContentType());
    }

    static Stream<Arguments> provideExceptions() throws Exception {
        return Stream.of(
                Arguments.of(new RuntimeException(), 500),
                Arguments.of(new RuntimeException("Bad", new PathNotFoundException()), 404));
    }

}
