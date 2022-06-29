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
import java.util.Optional;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.servlets.json.problem.Problem;
import org.apache.sling.servlets.json.problem.ProblemBuilder;
import org.apache.sling.servlets.json.problem.Problematic;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * An extension of the SlingAllMethodsServlet tailored to producing JSON APIs.
 * <p>
 * This class adds support for PATCH requests and adds several different useful
 * base methods for reading the response body as JSON, writing an object as JSON
 * and sending problems as RFC 7807-compliant JSON+Problem responses
 * <p>
 * This class also catches ServletException, IOException and
 * RuntimeExceptions thrown from the called methods and sends a JSON
 * Problem response based on the thrown exception
 */
@ConsumerType
public abstract class BaseJsonServlet extends HttpServlet {

    private static final String RESPONSE_CONTENT_TYPE = "application/json";

    private static final Set<String> SERVLET_SUPPORTED_METHODS = Set.of("GET", "HEAD", "POST", "PUT", "DELETE",
            "OPTIONS", "TRACE");

    private static final Logger log = LoggerFactory.getLogger(BaseJsonServlet.class);

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
     * Retrieves a <code>ResourceResolver</code> that can be used to perform various
     * operations against the underlying repository.
     *
     * @return Resolver for performing operations. Will not be null.
     * @throws LoginException unable to find resource resolver in request
     */
    public @NotNull ResourceResolver getResourceResolver(@NotNull HttpServletRequest request) throws LoginException {
        return Optional.ofNullable(request.getAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER))
                .map(ResourceResolver.class::cast)
                .orElseThrow(() -> new LoginException("Could not get ResourceResolver from request"));
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
     * Read an object from the request, handing invalid or missing request bodies
     * and returning a 400 response.
     *
     * @param <T>     the type of object to be read from the request
     * @param request the request from which to read the object
     * @param type    the class of the type to read
     * @return the object read from the request
     */
    protected abstract <T> T readRequestBody(HttpServletRequest request, Class<T> type);

    /**
     * Read an object from the request, handing invalid or missing request bodies
     * and returning a 400 response.
     *
     * @param <T>     the type of object to be read from the request
     * @param request the request from which to read the object
     * @param type    the class of the type to read
     * @return the object read from the request
     */
    protected abstract <T> T readRequestBody(HttpServletRequest request, TypeReference<T> type);

    /**
     * Sends a JSON response with the content type application/json and a 200 status
     * code.
     *
     * @param response     the response to which to write
     * @param responseBody the object to write to the response
     * @throws IOException an exception occurs writing the object to the response
     */
    protected void sendJsonResponse(HttpServletResponse response, Object responseBody)
            throws IOException {
        sendJsonResponse(response, HttpServletResponse.SC_OK, responseBody);
    }

    /**
     * Sends a JSON response with the content type application/json
     *
     * @param response     the response to which to write
     * @param statusCode   the status code to send for the response
     * @param responseBody the object to write to the response
     * @throws IOException an exception occurs writing the object to the response
     */
    protected void sendJsonResponse(HttpServletResponse response, int statusCode, Object responseBody)
            throws IOException {
        sendJsonResponse(response, statusCode, RESPONSE_CONTENT_TYPE, responseBody);
    }

    /**
     * Sends a JSON response by serializing the responseBody object into JSON
     *
     * @param response     the response to which to write
     * @param statusCode   the status code to send for the response
     * @param contentType  the content type to send for the response
     * @param responseBody the object to write to the response
     * @throws IOException an exception occurs writing the object to the response
     */
    protected abstract void sendJsonResponse(HttpServletResponse response, int statusCode, String contentType,
            Object responseBody) throws IOException;

    /**
     * Sends a problem response, setting the status based on the status of the
     * ProblemBuilder, the content type application/problem+json and the body being
     * the the problem JSON
     *
     * @param response       the response to which to write the problem
     * @param problemBuilder the problem to write
     * @throws IOException Thrown if the problem cannot be written to the response
     */
    protected void sendProblemResponse(HttpServletResponse response, Problem problem)
            throws IOException {
        sendJsonResponse(response, problem.getStatus(), ProblemBuilder.RESPONSE_CONTENT_TYPE,
                problem);
    }

    /**
     * Helper method which causes an method not allowed HTTP and JSON problem
     * response to be sent for an unhandled HTTP request method.
     *
     * @param request  Required for method override
     * @param response The HTTP response to which the error status is sent.
     * @throws IOException Thrown if the status cannot be sent to the client.
     */
    protected void handleMethodNotImplemented(@NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response) throws IOException {
        sendProblemResponse(response,
                ProblemBuilder.get().withStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED).build());
    }
}
