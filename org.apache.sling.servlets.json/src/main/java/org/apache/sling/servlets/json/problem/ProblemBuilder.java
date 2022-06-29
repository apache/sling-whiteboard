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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidLifecycleTransitionException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
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
import javax.jcr.version.VersionException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import com.fasterxml.jackson.databind.ObjectMapper;

@ProviderType
public class ProblemBuilder {

    public static final String RESPONSE_CONTENT_TYPE = "application/problem+json";

    private static final String PN_TYPE = "type";
    private static final String PN_TITLE = "title";
    private static final String PN_STATUS = "status";
    private static final String PN_DETAIL = "detail";
    private static final String PN_INSTANCE = "instance";
    private int status;

    private static final Set<String> RESERVED_PROPERTIES = Set.of(PN_TYPE, PN_TITLE, PN_STATUS, PN_DETAIL, PN_INSTANCE);

    private Map<String, Object> properties = new HashMap<>();

    public static ProblemBuilder get() {
        return new ProblemBuilder();
    }

    private ProblemBuilder() {
        withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public ProblemBuilder withType(@NotNull final URI type) {
        properties.put(PN_TYPE, type);
        return this;
    }

    public ProblemBuilder withTitle(@NotNull final String title) {
        properties.put(PN_TITLE, title);
        return this;
    }

    public ProblemBuilder withStatus(@NotNull final int status) {
        properties.put(PN_STATUS, status);
        this.status = status;
        return this;
    }

    public ProblemBuilder fromException(Exception ex) {
        if (ex instanceof SlingException) {
            return fromSlingException((SlingException) ex);
        }
        if (ex instanceof RepositoryException) {
            return fromRepositoryException((RepositoryException) ex);
        }
        if (ex instanceof org.apache.sling.api.resource.LoginException) {
            withStatus(HttpServletResponse.SC_UNAUTHORIZED);
            withDetail(ex.toString());
        } else if (ex.getCause() instanceof Exception) {
            fromException((Exception) ex.getCause());
            withDetail(ex.toString() + "\nCause: " + properties.get(PN_DETAIL));
        } else {
            withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            withDetail(ex.toString());
        }
        return this;
    }

    public ProblemBuilder fromSlingException(@NotNull SlingException exception) {
        if (exception instanceof ResourceNotFoundException) {
            withStatus(HttpServletResponse.SC_NOT_FOUND);
        } else if (exception instanceof QuerySyntaxException) {
            withStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        withDetail(exception.toString());
        return this;
    }

    public ProblemBuilder fromRepositoryException(@NotNull RepositoryException exception) {
        if (exception instanceof InvalidQueryException
                || exception instanceof NoSuchNodeTypeException
                || exception instanceof ConstraintViolationException
                || exception instanceof InvalidLifecycleTransitionException
                || exception instanceof InvalidNodeTypeDefinitionException
                || exception instanceof InvalidSerializedDataException
                || exception instanceof ReferentialIntegrityException
                || exception instanceof UnsupportedRepositoryOperationException
                || exception instanceof ValueFormatException
                || exception instanceof VersionException) {
            withStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else if (exception instanceof javax.jcr.LoginException) {
            withStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else if (exception instanceof javax.jcr.AccessDeniedException
                || exception instanceof javax.jcr.security.AccessControlException) {
            withStatus(HttpServletResponse.SC_FORBIDDEN);
        } else if ((exception instanceof ItemNotFoundException)
                || (exception instanceof PathNotFoundException)
                || exception instanceof NoSuchWorkspaceException) {
            withStatus(HttpServletResponse.SC_NOT_FOUND);
        } else if (exception instanceof ItemExistsException
                || exception instanceof InvalidItemStateException
                || exception instanceof LockException
                || exception instanceof MergeException
                || exception instanceof NodeTypeExistsException) {
            withStatus(HttpServletResponse.SC_CONFLICT);
        } else {
            withStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        withDetail(exception.toString());
        return this;
    }

    public ProblemBuilder withDetail(@NotNull final String detail) {
        properties.put(PN_DETAIL, detail);
        return this;
    }

    public ProblemBuilder withInstance(@NotNull final URI instance) {
        properties.put(PN_INSTANCE, instance);
        return this;
    }

    public ProblemBuilder with(final String key, @NotNull final Object value) throws IllegalArgumentException {
        if (RESERVED_PROPERTIES.contains(key)) {
            throw new IllegalArgumentException("Property " + key + " is reserved");
        }
        properties.put(key, value);
        return this;
    }

    public int getStatus() {
        return status;
    }

    public static String statusToString(int statusCode) {
        switch (statusCode) { // NOSONAR
            case 100:
                return "Continue";
            case 101:
                return "Switching Protocols";
            case 102:
                return "Processing (WebDAV)";
            case 200:
                return "OK";
            case 201:
                return "Created";
            case 202:
                return "Accepted";
            case 203:
                return "Non-Authoritative Information";
            case 204:
                return "No Content";
            case 205:
                return "Reset Content";
            case 206:
                return "Partial Content";
            case 207:
                return "Multi-Status (WebDAV)";
            case 300:
                return "Multiple Choices";
            case 301:
                return "Moved Permanently";
            case 302:
                return "Found";
            case 303:
                return "See Other";
            case 304:
                return "Not Modified";
            case 305:
                return "Use Proxy";
            case 307:
                return "Temporary Redirect";
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 402:
                return "Payment Required";
            case 403:
                return "Forbidden";
            case 404:
                return "Not Found";
            case 405:
                return "Method Not Allowed";
            case 406:
                return "Not Acceptable";
            case 407:
                return "Proxy Authentication Required";
            case 408:
                return "Request Time-out";
            case 409:
                return "Conflict";
            case 410:
                return "Gone";
            case 411:
                return "Length Required";
            case 412:
                return "Precondition Failed";
            case 413:
                return "Request Entity Too Large";
            case 414:
                return "Request-URI Too Large";
            case 415:
                return "Unsupported Media Type";
            case 416:
                return "Requested range not satisfiable";
            case 417:
                return "Expectation Failed";
            case 422:
                return "Unprocessable Entity (WebDAV)";
            case 423:
                return "Locked (WebDAV)";
            case 424:
                return "Failed Dependency (WebDAV)";
            case 500:
                return "Internal Server Error";
            case 501:
                return "Not Implemented";
            case 502:
                return "Bad Gateway";
            case 503:
                return "Service Unavailable";
            case 504:
                return "Gateway Time-out";
            case 505:
                return "HTTP Version not supported";
            case 507:
                return "Insufficient Storage (WebDAV)";
            case 510:
                return "Not Extended";
            default:
                return String.valueOf(statusCode);
        }
    }

    public Problem build() {
        properties.computeIfAbsent(PN_TITLE, k -> statusToString(getStatus()));
        return new ObjectMapper().convertValue(properties, Problem.class);
    }

    public ThrowableProblem buildThrowable() {
        return new ThrowableProblem(build());
    }

}
