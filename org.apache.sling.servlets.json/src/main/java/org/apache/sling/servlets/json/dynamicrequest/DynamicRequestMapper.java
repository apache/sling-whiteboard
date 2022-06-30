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
package org.apache.sling.servlets.json.dynamicrequest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlets.json.DynamicRequestServlet;
import org.apache.sling.servlets.json.annotations.RequestBody;
import org.apache.sling.servlets.json.annotations.RequestHandler;
import org.apache.sling.servlets.json.annotations.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicRequestMapper {

    private static final Logger log = LoggerFactory.getLogger(DynamicRequestMapper.class);

    private final Set<String> mappingKeys = new HashSet<>();
    private final Map<String, List<DynamicRequestMapping>> mappings = new HashMap<>();
    private final DynamicRequestServlet instance;

    public DynamicRequestMapper(DynamicRequestServlet instance) {
        readRequestHandlers(instance);
        this.instance = instance;
    }

    public boolean mayService(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        final String method = request.getMethod();
        final String path = request.getRequestURI();
        Optional<DynamicRequestMapping> mappingOp = Optional.ofNullable(mappings.get(method))
                .orElse(Collections.emptyList()).stream().filter(drm -> drm.matches(path)).findFirst();
        if (mappingOp.isPresent()) {
            callMethod(request, response, mappingOp.get());
            return true;
        } else {
            return false;
        }
    }

    private void callMethod(final HttpServletRequest request, final HttpServletResponse response,
            final DynamicRequestMapping dynamicRequestMapping) throws IOException, ServletException {
        final Method method = dynamicRequestMapping.getMethod();
        final List<Object> values = new ArrayList<>();
        final ValueMap parameters = new ValueMapDecorator(request.getParameterMap());
        for (Parameter param : method.getParameters()) {
            if (ServletRequest.class.isAssignableFrom(param.getType())) {
                log.trace("Adding request for param: {}", param);
                values.add(request);
            } else if (ServletResponse.class.isAssignableFrom(param.getType())) {
                log.trace("Adding response for param: {}", param);
                values.add(response);
            } else if (param.isAnnotationPresent(RequestBody.class)) {
                log.debug("Adding response body as {} for param: {}", param.getType(), param);
                values.add(instance.readRequestBody(request, param.getType()));
            } else if (param.isAnnotationPresent(RequestParameter.class)) {
                RequestParameter rp = param.getAnnotation(RequestParameter.class);
                log.debug("Adding response parameter {} as {} for param: {}", rp.name(), param.getType(), param);
                values.add(parameters.get(rp.name(), param.getType()));
            } else {
                throw new RequestMappingException("Failed to call : " + dynamicRequestMapping
                        + " parameter " + param.getName()
                        + " must either be a ServletRequest, ServletResponse or be annotated with request value mappings");
            }
        }

        try {
            log.trace("Invoking method {} with parameters {}", method, values);
            Object value = method.invoke(instance, values.toArray());
            if (value != null) {
                log.trace("Recieved response {}", value);
                instance.sendJsonResponse(response, value);
            }
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RequestMappingException("Unexpected exception invoking method " + method.toGenericString(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof ServletException) {
                throw (ServletException) cause;
            }
            throw new RequestMappingException("Unexpected exception invoking method " + method.toGenericString(), e);
        }
    }

    private void readRequestHandlers(HttpServlet instance) {
        log.debug("Loading request handlers from: {}", instance.getClass());
        for (Method method : instance.getClass().getDeclaredMethods()) {
            log.trace("Evaluating method: {}", method);
            RequestHandler handler = method.getAnnotation(RequestHandler.class);
            if (handler != null) {
                readRequestHandler(handler, method);
            }
        }
    }

    private void readRequestHandler(RequestHandler handler, Method method) {
        log.trace("Found request handler {} in method: {}", handler, method);
        DynamicRequestMapping mapping = new DynamicRequestMapping(handler, method);
        validateMapping(mapping);
        Arrays.stream(handler.methods()).forEach(m -> {

            log.trace("Adding request handler {} for method: {}", mapping, m);
            mappings.computeIfAbsent(m, k -> new ArrayList<>());
            mappings.get(m).add(mapping);
        });
    }

    private Collection<String> getMappingKeys(RequestHandler handler) {
        return Arrays.stream(handler.methods()).map(m -> '[' + m + "] " + handler.path()).collect(Collectors.toList());
    }

    private void validateMapping(DynamicRequestMapping mapping) {
        // validate that there aren't duplicate request handlers
        log.trace("Validing that requst handler is duplicate");
        getMappingKeys(mapping.getHandler()).forEach(kp -> {
            if (mappingKeys.contains(kp)) {
                throw new RequestMappingException("Validation failed for method: " + mapping.getMethod()
                        + " a request handler is already registred for " + kp);
            }
            mappingKeys.add(kp);
        });

        log.trace("Validing request handler can be injecteds");
        validateCanInject(mapping.getMethod());
    }

    private void validateCanInject(Method method) {
        for (Parameter param : method.getParameters()) {
            if (!ServletRequest.class.isAssignableFrom(param.getType())
                    && !ServletResponse.class.isAssignableFrom(param.getType())
                    && !param.isAnnotationPresent(RequestBody.class)
                    && !param.isAnnotationPresent(RequestParameter.class)) {
                throw new RequestMappingException("Validation failed for method: " + method
                        + " parameter " + param.getName()
                        + " must either be a ServletRequest, ServletResponse or be annotated with request value mappings");
            }
        }
    }

}
