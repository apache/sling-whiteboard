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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.servlets.json.problem.ProblemBuilder;
import org.apache.sling.servlets.json.problem.Problematic;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * An extension of the BaseJsonServlet using Jackson for serialization.
 */
@ConsumerType
public abstract class JacksonJsonServlet extends HttpServlet implements BaseJsonServlet {

    private static final Logger log = LoggerFactory.getLogger(JacksonJsonServlet.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter objectWriter = objectMapper.writer();
    private static final ObjectReader objectReader = objectMapper.reader();

    /**
     * Called by the
     * {@link #service(HttpServletRequest, HttpServletResponse)} method
     * to handle an HTTP <em>GET</em> request.
     * <p>
     * This default implementation reports back to the client that the method is
     * not supported.
     * <p>
     * Implementations of this class should overwrite this method with their
     * implementation for the HTTP <em>PATCH</em> method support.
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException      If the error status cannot be reported back to the
     *                          client.
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        sendProblemResponse(resp, ProblemBuilder.get().withStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED).build());
    }

    /**
     * Called by the
     * {@link #service(HttpServletRequest, HttpServletResponse)} method
     * to handle an HTTP <em>POST</em> request.
     * <p>
     * This default implementation reports back to the client that the method is
     * not supported.
     * <p>
     * Implementations of this class should overwrite this method with their
     * implementation for the HTTP <em>PATCH</em> method support.
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException      If the error status cannot be reported back to the
     *                          client.
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        sendProblemResponse(resp, ProblemBuilder.get().withStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED).build());
    }

    /**
     * Called by the
     * {@link #service(HttpServletRequest, HttpServletResponse)} method
     * to handle an HTTP <em>PUT</em> request.
     * <p>
     * This default implementation reports back to the client that the method is
     * not supported.
     * <p>
     * Implementations of this class should overwrite this method with their
     * implementation for the HTTP <em>PATCH</em> method support.
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException      If the error status cannot be reported back to the
     *                          client.
     */
    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        sendProblemResponse(resp, ProblemBuilder.get().withStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED).build());
    }

    /**
     * Called by the
     * {@link #service(HttpServletRequest, HttpServletResponse)} method
     * to handle an HTTP <em>DELETE</em> request.
     * <p>
     * This default implementation reports back to the client that the method is
     * not supported.
     * <p>
     * Implementations of this class should overwrite this method with their
     * implementation for the HTTP <em>PATCH</em> method support.
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException      If the error status cannot be reported back to the
     *                          client.
     */
    @Override
    protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        sendProblemResponse(resp, ProblemBuilder.get().withStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED).build());
    }

    /**
     * Called by the
     * {@link #service(HttpServletRequest, HttpServletResponse)} method
     * to handle an HTTP <em>PATCH</em> request.
     * <p>
     * This default implementation reports back to the client that the method is
     * not supported.
     * <p>
     * Implementations of this class should overwrite this method with their
     * implementation for the HTTP <em>PATCH</em> method support.
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @throws ServletException Not thrown by this implementation.
     * @throws IOException      If the error status cannot be reported back to the
     *                          client.
     */
    protected void doPatch(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response)
            throws ServletException, IOException {
        handleMethodNotImplemented(request, response);
    }

    /**
     * Tries to handle the request by calling a Java method implemented for the
     * respective HTTP request method.
     * <p>
     * This implementation first calls the base class implementation and only if
     * the base class cannot dispatch will try to dispatch the supported methods
     * <em>PATCH</em>
     * <p>
     * In addition, this method catches ServletException, IOException and
     * RuntimeExceptions thrown from the called methods and sends a JSON
     * Problem response based on the thrown exception
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @return <code>true</code> if the requested method
     *         (<code>request.getMethod()</code>)
     *         is known. Otherwise <code>false</code> is returned.
     * @throws ServletException Forwarded from any of the dispatched methods
     * @throws IOException      Forwarded from any of the dispatched methods
     */
    @Override
    protected void service(@NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response) throws ServletException,
            IOException {
        final String method = request.getMethod();
        try {
            // assume the method is known for now
            if (SERVLET_SUPPORTED_METHODS.contains(method)) {
                super.service(request, response);
            } else if ("PATCH".equals(method)) {
                doPatch(request, response);
            } else {
                handleMethodNotImplemented(request, response);
            }
        } catch (IOException | ServletException | RuntimeException e) {
            if (e instanceof Problematic) {
                sendProblemResponse(response, ((Problematic) e).getProblem());
            } else {
                log.error("Handing uncaught exception", e);
                sendProblemResponse(response, ProblemBuilder.get().fromException(e).build());
            }
        }
    }

    /**
     * Provides the Jackson ObjectWriter instance to use for writing objects to the
     * response.
     * <p>
     * Implementations of this class can overwrite this method to customize the
     * behavior of the ObjectWiter
     *
     * @return the ObjectWriter
     */
    public ObjectWriter getObjectWriter() {
        return objectWriter;
    }

    /**
     * Provides the Jackson ObjectReader instance to use for reading objects from
     * the request.
     * <p>
     * Implementations of this class can overwrite this method to customize the
     * behavior of the ObjectReader
     *
     * @return the ObjectReader
     */
    public ObjectReader getObjectReader() {
        return objectReader;
    }

    /**
     * Read an object from the request, handing invalid or missing request bodies
     * and returning a 400 response.
     *
     * @param <T>     the type of object to be read from the request
     * @param request the request from which to read the object
     * @param type    the class of the type to read
     * @return the object read from the request
     */
    @Override
    public <T> T readRequestBody(HttpServletRequest request, Class<T> type) {
        try {
            return getObjectReader().readValue(request.getReader(), type);
        } catch (IOException e) {
            throw ProblemBuilder.get().withStatus(HttpServletResponse.SC_BAD_REQUEST)
                    .withDetail("Unable to parse request as JSON: " + e.getMessage()).buildThrowable();
        }
    }

    /**
     * Read an object from the request, handing invalid or missing request bodies
     * and returning a 400 response.
     *
     * @param <T>     the type of object to be read from the request
     * @param request the request from which to read the object
     * @param type    the class of the type to read
     * @return the object read from the request
     */
    @Override
    public <T> T readRequestBody(HttpServletRequest request, TypeReference<T> type) {
        try {
            return getObjectReader().forType(type).readValue(request.getReader());
        } catch (IOException e) {
            throw ProblemBuilder.get().withStatus(HttpServletResponse.SC_BAD_REQUEST)
                    .withDetail("Unable to parse request as JSON: " + e.getMessage()).buildThrowable();
        }
    }

    /**
     * Sends a JSON response
     *
     * @param response     the response to which to write
     * @param statusCode   the status code to send for the response
     * @param contentType  the content type to send for the response
     * @param responseBody the object to write to the response
     * @throws IOException an exception occurs writing the object to the response
     */
    @Override
    public void sendJsonResponse(HttpServletResponse response, int statusCode, String contentType,
            Object responseBody) throws IOException {
        if (!response.isCommitted()) {
            response.reset();
            response.setStatus(statusCode);
            response.setContentType(contentType);
            response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        } else {
            // Response already committed: don't change status
            log.warn("Response already committed, unable to change status, output might not be well formed");
        }
        response.getWriter().write(getObjectWriter().writeValueAsString(responseBody));
    }

}
