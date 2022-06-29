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
package org.apache.sling.servlets.json.problem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidLifecycleTransitionException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.security.AccessControlException;
import javax.jcr.version.VersionException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class ProblemBuilderTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void noParams() throws JsonProcessingException {
        Problem built = ProblemBuilder.get().build();
        assertEquals(Collections.emptyMap(), built.getCustom());
        assertNull(built.getDetail());
        assertNull(built.getInstance());
        assertEquals(500, built.getStatus());
        assertEquals("Internal Server Error", built.getTitle());
        assertNull(built.getType());

        assertEquals("{\"title\":\"Internal Server Error\",\"status\":500}", objectMapper.writeValueAsString(built));
    }

    @Test
    void supportsStatusOnly() throws JsonProcessingException {
        Problem built = ProblemBuilder.get().withStatus(HttpServletResponse.SC_GONE).build();
        assertEquals(Collections.emptyMap(), built.getCustom());
        assertNull(built.getDetail());
        assertNull(built.getInstance());
        assertEquals(410, built.getStatus());
        assertEquals("Gone", built.getTitle());
        assertNull(built.getType());
        assertEquals("{\"title\":\"Gone\",\"status\":410}", objectMapper.writeValueAsString(built));
    }

    @Test
    void supportsAllProps() {
        Problem built = ProblemBuilder.get().withStatus(HttpServletResponse.SC_GONE).withDetail("DETAIL")
                .withInstance(URI.create("https://www.apache.org/")).withTitle(
                        "TITLE")
                .withType(URI.create("https://sling.apache.org/")).build();
        assertEquals(Collections.emptyMap(), built.getCustom());
        assertEquals("DETAIL", built.getDetail());
        assertEquals(URI.create("https://www.apache.org/"), built.getInstance());
        assertEquals(410, built.getStatus());
        assertEquals("TITLE", built.getTitle());
        assertEquals(URI.create("https://sling.apache.org/"), built.getType());
    }

    @Test
    void supportsCustom() throws JsonProcessingException {
        Problem built = ProblemBuilder.get().with("test", "value").build();
        assertEquals("value", built.getCustom().get("test"));
        assertEquals("{\"title\":\"Internal Server Error\",\"status\":500,\"test\":\"value\"}",
                objectMapper.writeValueAsString(built));
    }

    @Test
    void protectsReservedProperties() throws JsonProcessingException {
        assertThrows(IllegalArgumentException.class, () -> ProblemBuilder.get().with("type", "test"));
    }

    @Test
    void assertStatusCodesMaptoStrings() {
        for (int i = 100; i < 600; i++) {
            Problem problem = ProblemBuilder.get().withStatus(i).build();
            assertNotNull(problem.getTitle());
        }
    }

    @Test
    void canGetThrowableProblem() throws JsonProcessingException {
        ProblemBuilder builder = ProblemBuilder.get().with("test", "value");
        Problem built = builder.build();
        assertEquals(objectMapper.writeValueAsString(built),
                objectMapper.writeValueAsString(builder.buildThrowable().getProblem()));
    }

    @ParameterizedTest
    @MethodSource("provideExceptions")
    void supportsWithException(Exception exception, int status, String title, String detail) {
        Problem built = ProblemBuilder.get().fromException(exception).build();
        assertEquals(detail, built.getDetail());
        assertEquals(status, built.getStatus());
        assertEquals(title, built.getTitle());
    }

    static Arguments createArg(Exception ex, int status, String title) throws Exception {
        return Arguments.of(ex, status, title, ex.toString());
    }

    static Stream<Arguments> provideExceptions() throws Exception {

        List<Arguments> testContent = List.of(

                // 400's
                createArg(new InvalidQueryException("Bad"), 400, "Bad Request"),
                createArg(new NoSuchNodeTypeException("Bad"), 400, "Bad Request"),
                createArg(new ConstraintViolationException("Bad"), 400, "Bad Request"),
                createArg(new InvalidLifecycleTransitionException("Bad"), 400, "Bad Request"),
                createArg(new InvalidNodeTypeDefinitionException("Bad"), 400, "Bad Request"),
                createArg(new InvalidSerializedDataException("Bad"), 400, "Bad Request"),
                createArg(new ReferentialIntegrityException("Bad"), 400, "Bad Request"),
                createArg(new UnsupportedRepositoryOperationException("Bad"), 400, "Bad Request"),
                createArg(new ValueFormatException("Bad"), 400, "Bad Request"),
                createArg(new VersionException("Bad"), 400, "Bad Request"),
                createArg(new QuerySyntaxException("Bad", "SELECT * FROM [nt:file]", Query.JCR_SQL2), 400,
                        "Bad Request"),

                // 401's
                createArg(new LoginException("Bad"), 401, "Unauthorized"),
                createArg(new org.apache.sling.api.resource.LoginException("Bad"), 401, "Unauthorized"),

                // 403's
                createArg(new AccessDeniedException("Bad"), 403, "Forbidden"),
                createArg(new AccessControlException("Bad"), 403, "Forbidden"),

                // 404's
                createArg(new ResourceNotFoundException("/content", "Bad"), 404, "Not Found"),
                createArg(new ItemNotFoundException("Bad"), 404, "Not Found"),
                createArg(new PathNotFoundException("Bad"), 404, "Not Found"),
                createArg(new NoSuchWorkspaceException("Bad"), 404, "Not Found"),

                // 409's
                createArg(new ItemExistsException("Bad"), 409, "Conflict"),
                createArg(new InvalidItemStateException("Bad"), 409, "Conflict"),
                createArg(new LockException("Bad"), 409, "Conflict"),
                createArg(new MergeException("Bad"), 409, "Conflict"),
                createArg(new NodeTypeExistsException("Bad"), 409, "Conflict"),

                // 500's
                createArg(new RepositoryException("Bad"), 500, "Internal Server Error"),
                createArg(new SlingException("Bad", new Exception()), 500, "Internal Server Error"),
                createArg(new IOException("Bad"), 500, "Internal Server Error"));

        return Stream.concat(testContent.stream(), testContent.stream().map(a -> {
            Object[] args = a.get();
            return Arguments.of(new Exception("Wrapped", (Exception) args[0]), args[1], args[2],
                    "java.lang.Exception: Wrapped\nCause: " + args[3]);
        }));
    }
}
