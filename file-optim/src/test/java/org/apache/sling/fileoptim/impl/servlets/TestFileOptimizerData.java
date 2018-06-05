/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.fileoptim.impl.servlets;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.fileoptim.BaseFileOptimizerTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFileOptimizerData extends BaseFileOptimizerTest {

	public TestFileOptimizerData() throws InvalidSyntaxException {
		super();
		log.trace("TestFileOptimizerData()");
	}

	private static final Logger log = LoggerFactory.getLogger(TestFileOptimizerData.class);
	private static final String PATH = "/content/file.jpg";
	private StringWriter writer;
	private SlingHttpServletRequest request;
	private SlingHttpServletResponse response;
	private FileOptimizerData servlet;

	@Before
	public void initTest() throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException,
			IllegalAccessException {
		log.trace("initTest");
		request = Mockito.mock(SlingHttpServletRequest.class);
		Mockito.when(request.getParameter("path")).thenReturn(PATH);
		ResourceResolver resolver = Mockito.mock(ResourceResolver.class);
		Mockito.when(request.getResourceResolver()).thenReturn(resolver);
		Resource jpegResource = getValidJpegFile();
		Mockito.when(resolver.getResource(PATH)).thenReturn(jpegResource);

		this.writer = new StringWriter();
		response = Mockito.mock(SlingHttpServletResponse.class);
		Mockito.when(response.getWriter()).thenReturn(new PrintWriter(writer));

		servlet = new FileOptimizerData();
		Field fo = FileOptimizerData.class.getDeclaredField("fileOptimizer");
		fo.setAccessible(true);
		fo.set(servlet, this.fileOptimizerService);
	}

	@Test
	public void testServlet() throws ServletException, IOException {

		servlet.doGet(request, response);

		String response = writer.toString();
		assertNotNull(response);
		log.info(response);
		
	}
}
