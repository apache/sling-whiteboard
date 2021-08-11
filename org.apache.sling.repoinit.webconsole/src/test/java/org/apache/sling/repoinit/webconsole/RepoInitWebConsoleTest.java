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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RepoInitWebConsoleTest {

    @Rule
    public SlingContext context = new SlingContext();
    private RepoInitParser parser;
    private JcrRepoInitOpsProcessor processor;
    private RepoInitWebConsole webConsole;

    @Before
    public void setup() throws RepoInitParsingException, LoginException, RepositoryException, IOException {
        context.request().setServletPath("/system/console/");
        this.parser = mock(RepoInitParser.class);
        context.registerService(RepoInitParser.class, parser);
        this.processor = mock(JcrRepoInitOpsProcessor.class);
        when(parser.parse(any())).thenReturn(Collections.emptyList());
        context.registerService(JcrRepoInitOpsProcessor.class, processor);

        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.adaptTo(Session.class)).thenReturn(mock(Session.class));

        context.request().setAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER, resolver);

        webConsole = new RepoInitWebConsole();

        webConsole.activate(context.componentContext());

    }

    public void testDoGetRequest(String resource, String type) throws ServletException, IOException {
        context.request().setPathInfo("repoinit/" + resource);
        webConsole.doGet(context.request(), context.response());
        assertEquals(200, context.response().getStatus());
        assertEquals(type, context.response().getContentType());
        assertEquals(IOUtils.toString(getClass().getClassLoader().getResource(resource), StandardCharsets.UTF_8),
                context.response().getOutputAsString());
    }

    @Test
    public void testGetRepoinit() throws ServletException, IOException {
        testDoGetRequest("res/ui/repoinit.js", "application/javascript");
    }

    @Test
    public void testGetTnlJS() throws ServletException, IOException {
        testDoGetRequest("res/ui/tln.min.js", "application/javascript");
    }

    @Test
    public void testGetTnlCss() throws ServletException, IOException {
        testDoGetRequest("res/ui/tln.min.css", "text/css");
    }

    @Test
    public void testGetHtml() throws ServletException, IOException {
        context.request().setPathInfo("repoinit");
        webConsole.doGet(context.request(), context.response());
        assertEquals(200, context.response().getStatus());
        assertEquals("text/html;charset=utf-8", context.response().getContentType());
        assertNotEquals(-1, context.response().getOutputAsString().indexOf(
                IOUtils.toString(getClass().getClassLoader().getResource("tpl/main.html"), StandardCharsets.UTF_8)));
    }

    @Test
    public void testConsoleInfo() throws IOException {
        assertEquals(RepoInitWebConsole.CONSOLE_LABEL, webConsole.getLabel());
        assertEquals(RepoInitWebConsole.CONSOLE_TITLE, webConsole.getTitle());
    }

    @Test
    public void testPostValid() throws ServletException, IOException {

        context.request().setContent("create user".getBytes());
        webConsole.doPost(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("application/json", context.response().getContentType());
        assertEquals("{\"succeeded\":true,\"operations\":[],\"messages\":[\"Parsed Repoinit script successfully!\"]}",
                context.response().getOutputAsString());
    }

    @Test
    public void testPostFailure() throws ServletException, IOException, RepoInitParsingException {
        when(parser.parse(any())).thenThrow(new RepoInitParsingException("Failed because bad", null));

        context.request().setContent("create user".getBytes());
        webConsole.doPost(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
        assertEquals("application/json", context.response().getContentType());
        assertEquals(
                "{\"succeeded\":false,\"messages\":[\"Failed to parse RepoInit script:  [RepoInitParsingException]: Failed because bad\"]}",
                context.response().getOutputAsString());
    }

    @Test
    public void testPostExecute() throws ServletException, IOException, RepoInitParsingException {

        context.request().setContent("create user".getBytes());
        context.request().addRequestParameter("execute", "true");
        webConsole.doPost(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("application/json", context.response().getContentType());
        assertEquals(
                "{\"succeeded\":true,\"operations\":[],\"messages\":[\"Parsed Repoinit script successfully!\",\"Executed statements successfully!\"]}",
                context.response().getOutputAsString());
        verify(processor).apply(any(), any());
    }

    @Test
    public void testPostExecuteFailure() throws ServletException, IOException, RepoInitParsingException {
        doThrow(new RuntimeException("ERROR")).when(processor).apply(any(), any());

        context.request().setContent("create user".getBytes());
        context.request().addRequestParameter("execute", "true");
        webConsole.doPost(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
        assertEquals("application/json", context.response().getContentType());
        assertEquals(
                "{\"succeeded\":false,\"operations\":[],\"messages\":[\"Parsed Repoinit script successfully!\",\"Failed to apply statements [RuntimeException]: ERROR\"]}",
                context.response().getOutputAsString());

    }

    @Test
    public void testNoResourceResolver() throws ServletException, IOException, RepoInitParsingException {

        context.request().setAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER, null);

        context.request().setContent("create user".getBytes());
        context.request().addRequestParameter("execute", "true");
        webConsole.doPost(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
        assertEquals("application/json", context.response().getContentType());
        assertEquals(
                "{\"succeeded\":false,\"operations\":[],\"messages\":[\"Parsed Repoinit script successfully!\",\"Failed to apply statements [IllegalStateException]: No resource resolver available\"]}",
                context.response().getOutputAsString());

    }

    @Test
    public void testNoSession() throws ServletException, IOException, RepoInitParsingException {

        context.request().setAttribute(AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER, mock(ResourceResolver.class));

        context.request().setContent("create user".getBytes());
        context.request().addRequestParameter("execute", "true");
        webConsole.doPost(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
        assertEquals("application/json", context.response().getContentType());
        assertEquals(
                "{\"succeeded\":false,\"operations\":[],\"messages\":[\"Parsed Repoinit script successfully!\",\"Failed to apply statements [IllegalStateException]: No session available\"]}",
                context.response().getOutputAsString());

    }
}
