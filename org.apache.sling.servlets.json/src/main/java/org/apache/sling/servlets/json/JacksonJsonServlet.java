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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.servlets.json.problem.ProblemBuilder;
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
public abstract class JacksonJsonServlet extends BaseJsonServlet {

    private static final Logger log = LoggerFactory.getLogger(JacksonJsonServlet.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter objectWriter = objectMapper.writer();
    private static final ObjectReader objectReader = objectMapper.reader();

    /**
     * Provides the Jackson ObjectWriter instance to use for writing objects to the
     * response.
     * <p>
     * Implementations of this class can overwrite this method to customize the
     * behavior of the ObjectWiter
     *
     * @return the ObjectWriter
     */
    protected ObjectWriter getObjectWriter() {
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
    protected ObjectReader getObjectReader() {
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
    protected <T> T readRequestBody(HttpServletRequest request, Class<T> type) {
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
    protected <T> T readRequestBody(HttpServletRequest request, TypeReference<T> type) {
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
    protected void sendJsonResponse(HttpServletResponse response, int statusCode, String contentType,
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
