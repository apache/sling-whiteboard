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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.servlets.json.problem.Problem;
import org.apache.sling.servlets.json.problem.ProblemBuilder;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

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
public interface BaseJsonServlet {

    static final String RESPONSE_CONTENT_TYPE = "application/json";

    static final Set<String> SERVLET_SUPPORTED_METHODS = Set.of("GET", "HEAD", "POST", "PUT", "DELETE",
            "OPTIONS", "TRACE");

    /**
     * Retrieves a <code>ResourceResolver</code> that can be used to perform various
     * operations against the underlying repository.
     *
     * @return Resolver for performing operations. Will not be null.
     * @throws LoginException unable to find resource resolver in request
     */
    default @NotNull ResourceResolver getResourceResolver(@NotNull HttpServletRequest request) throws LoginException {
        return Optional.ofNullable(request.getAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER))
                .map(ResourceResolver.class::cast)
                .orElseThrow(() -> new LoginException("Could not get ResourceResolver from request"));
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
    <T> T readRequestBody(HttpServletRequest request, Class<T> type);

    /**
     * Read an object from the request, handing invalid or missing request bodies
     * and returning a 400 response.
     *
     * @param <T>     the type of object to be read from the request
     * @param request the request from which to read the object
     * @param type    the class of the type to read
     * @return the object read from the request
     */
    <T> T readRequestBody(HttpServletRequest request, TypeReference<T> type);

    /**
     * Sends a JSON response with the content type application/json and a 200 status
     * code.
     *
     * @param response     the response to which to write
     * @param responseBody the object to write to the response
     * @throws IOException an exception occurs writing the object to the response
     */
    default void sendJsonResponse(HttpServletResponse response, Object responseBody)
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
    default void sendJsonResponse(HttpServletResponse response, int statusCode, Object responseBody)
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
    void sendJsonResponse(HttpServletResponse response, int statusCode, String contentType,
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
    default void sendProblemResponse(HttpServletResponse response, Problem problem)
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
    default void handleMethodNotImplemented(@NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response) throws IOException {
        sendProblemResponse(response,
                ProblemBuilder.get().withStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED).build());
    }
}
