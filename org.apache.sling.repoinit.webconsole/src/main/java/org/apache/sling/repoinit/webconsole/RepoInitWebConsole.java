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
import java.nio.charset.StandardCharsets;
import java.util.List;

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
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(property = { Constants.SERVICE_DESCRIPTION + "=RepoInit Web Console Plugin",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        WebConsoleConstants.PLUGIN_LABEL + "=" + RepoInitWebConsole.CONSOLE_LABEL,
        WebConsoleConstants.PLUGIN_TITLE + "=" + RepoInitWebConsole.CONSOLE_TITLE,
        WebConsoleConstants.CONFIG_PRINTER_MODES + "=always",
        WebConsoleConstants.PLUGIN_CATEGORY + "=Status" }, service = { Servlet.class })
public class RepoInitWebConsole extends AbstractWebConsolePlugin {

    public static final String CONSOLE_LABEL = "repoinit";
    public static final String CONSOLE_TITLE = "Sling RepoInit";
    private final RepoInitParser parser;
    private final SlingRepository slingRepository;
    private final JcrRepoInitOpsProcessor processor;
    static final String RES_LOC = CONSOLE_LABEL + "/res/ui";

    @Activate
    public RepoInitWebConsole(@Reference RepoInitParser parser, @Reference SlingRepository slingRepository,
            @Reference JcrRepoInitOpsProcessor processor) throws IOException {
        this.parser = parser;
        this.processor = processor;
        this.slingRepository = slingRepository;
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
        if (request.getRequestURI().endsWith(RES_LOC + "/tln.min.css")) {
            response.setContentType("text/css");
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("res/ui/tln.min.css"),
                    response.getOutputStream());
        } else if (request.getRequestURI().endsWith(RES_LOC + "/tln.min.js")) {
            response.setContentType("application/javascript");
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("res/ui/tln.min.js"),
                    response.getOutputStream());
        } else if (request.getRequestURI().endsWith(RES_LOC + "/repoinit.js")) {
            response.setContentType("application/javascript");
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("res/ui/repoinit.js"),
                    response.getOutputStream());
        } else {
            super.doGet(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Operation> operations;
        try {
            operations = parser.parse(request.getReader());
        } catch (RepoInitParsingException e) {
            handleException(response, "Failed to parse RepoInit Statement: ", e);
            return;
        }
        ApiResponse apiResponse = new ApiResponse(operations);

        if ("true".equals(request.getParameter("execute"))) {
            try {
                processor.apply(slingRepository.loginAdministrative(null), operations);
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
        protected final List<Operation> operations;

        protected String errorMessage;

        public ApiResponse(List<Operation> operations) {
            this.succeeded = true;
            this.operations = operations;
            errorMessage = null;
        }

        public ApiResponse(String message, Exception e) {
            this.operations = null;
            setErrorMessage(message, e);
        }

        public boolean isSucceeded() {
            return succeeded;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public List<Operation> getOperations() {
            return operations;
        }

        public void setErrorMessage(String message, Exception e){
            this.succeeded = false;
            this.errorMessage = message + " [" + e.getClass().getSimpleName() + "]: " + e.getMessage();
        }
    }
}
