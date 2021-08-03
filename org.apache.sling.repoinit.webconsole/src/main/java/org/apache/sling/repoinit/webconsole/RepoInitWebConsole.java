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
package org.apache.sling.repoinit.webconsole;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(property = { Constants.SERVICE_DESCRIPTION + "=RepoInit Web Console Plugin",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        WebConsoleConstants.PLUGIN_LABEL + "=" + RepoInitWebConsole.CONSOLE_LABEL,
        WebConsoleConstants.PLUGIN_TITLE + "=" + RepoInitWebConsole.CONSOLE_TITLE,
        WebConsoleConstants.PLUGIN_CATEGORY + "=Sling" }, service = { Servlet.class })
public class RepoInitWebConsole extends AbstractWebConsolePlugin {

    public static final String CONSOLE_LABEL = "repoinit";
    public static final String CONSOLE_TITLE = "RepoInit";
    private static final String RES_LOC = CONSOLE_LABEL + "/res/ui/";

    private static final Map<String, String> RESOURCES = new HashMap<>();
    static {
        RESOURCES.put("repoinit.css", "text/css");
        RESOURCES.put("tln.min.css", "text/css");
        RESOURCES.put("tln.min.js", "application/javascript");
        RESOURCES.put("repoinit.js", "application/javascript");
    }

    @Reference
    protected SlingRepository slingRepository;

    private BundleContext context;

    @Activate
    public void activate(ComponentContext context) {
        this.context = context.getBundleContext();
    }

    @SuppressWarnings("unchecked")
    private List<?> parse(Reader reader) throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        ServiceReference<?> reference = context.getServiceReference("org.apache.sling.repoinit.parser.RepoInitParser");
        try {
            Object parser = context.getService(reference);
            Method method = parser.getClass().getDeclaredMethod("parse", Reader.class);
            return (List<Object>) method.invoke(parser, reader);
        } finally {
            context.ungetService(reference);
        }
    }

    private void process(Session session, List<?> operations) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        ServiceReference<?> reference = context
                .getServiceReference("org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor");
        try {
            Object processor = context.getService(reference);
            Method method = processor.getClass().getDeclaredMethod("apply", Session.class, List.class);
            method.invoke(processor, session, operations);
        } finally {
            context.ungetService(reference);
        }
    }

    @Override
    public String getLabel() {
        return CONSOLE_LABEL;
    }

    @Override
    public String getTitle() {
        return CONSOLE_TITLE;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        for (Entry<String, String> res : RESOURCES.entrySet()) {
            if (request.getRequestURI().endsWith(RES_LOC + res.getKey())) {
                response.setContentType(res.getValue());
                IOUtils.copy(getClass().getClassLoader().getResourceAsStream("res/ui/" + res.getKey()),
                        response.getOutputStream());
                return;
            }
        }
        super.doGet(request, response);
    }

    @SuppressWarnings("deprecated")
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<?> operations;
        try {
            operations = parse(request.getReader());
        } catch (InvocationTargetException e) {
            handleException(response, "Failed to parse RepoInit script: ", (Exception) e.getCause());
            return;
        } catch (Exception e) {
            handleException(response, "Failed to parse RepoInit script: ", e);
            return;
        }
        ApiResponse apiResponse = new ApiResponse("Parsed Repoinit script successfully!", operations);

        if ("true".equals(request.getParameter("execute"))) {
            try {

                process(slingRepository.loginAdministrative(null), operations);
                apiResponse.addMessage("Executed statements successfully!");
            } catch (InvocationTargetException e) {
                response.setStatus(400);
                apiResponse.setErrorMessage("Failed to apply statements", (Exception) e.getCause());
            } catch (Exception e) {
                response.setStatus(400);
                apiResponse.setErrorMessage("Failed to apply statements", e);
            }
        }

        writeResponse(response, apiResponse);
    }

    @Override
    protected void renderContent(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream("tpl/main.html"), res.getWriter(),
                StandardCharsets.UTF_8);
    }

    private void handleException(HttpServletResponse response, String message, Exception e) throws IOException {
        ApiResponse apiResponse = new ApiResponse(message, e);
        response.setStatus(400);
        writeResponse(response, apiResponse);
    }

    private void writeResponse(HttpServletResponse response, ApiResponse apiResponse)
            throws JsonGenerationException, JsonMappingException, IOException {
        response.setContentType("application/json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }

    @SuppressWarnings("unused")
    private class ApiResponse {
        protected boolean succeeded;

        @JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
        protected final List<?> operations;

        protected List<String> messages = new ArrayList<>();

        public ApiResponse(String message, List<?> operations) {
            this.succeeded = true;
            this.operations = operations;
            messages.add(message);
        }

        public ApiResponse(String message, Exception e) {
            this.operations = null;
            setErrorMessage(message, e);
        }

        public void addMessage(String message) {
            this.messages.add(message);
        }

        public boolean isSucceeded() {
            return succeeded;
        }

        public List<String> getMessages() {
            return messages;
        }

        public List<?> getOperations() {
            return operations;
        }

        public void setErrorMessage(String message, Exception e) {
            this.succeeded = false;
            this.messages.add(message + " [" + e.getClass().getSimpleName() + "]: " + e.getMessage());
        }
    }
}
