/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.fileoptim.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.fileoptim.FileOptimizer;
import org.apache.sling.fileoptim.FileOptimizerService;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Simple web console plugin for listing out the available optimizers
 */
@Component(property = { Constants.SERVICE_DESCRIPTION + "=Web Console Plugin for Apache Sling File Optimizer",
		Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
		WebConsoleConstants.PLUGIN_LABEL + "=" + FileOptimizerWebConsole.CONSOLE_LABEL,
		WebConsoleConstants.PLUGIN_TITLE + "=" + FileOptimizerWebConsole.CONSOLE_TITLE,
		WebConsoleConstants.CONFIG_PRINTER_MODES + "=always",
		WebConsoleConstants.PLUGIN_CATEGORY + "=Status" }, service = { Servlet.class })
public class FileOptimizerWebConsole extends AbstractWebConsolePlugin {

	private static final long serialVersionUID = 7086113364871387281L;
	public static final String CONSOLE_LABEL = "fileoptim";
	public static final String CONSOLE_TITLE = "File Optimizer";

	@Reference
	private FileOptimizerService fileOptimizer;

	@Override
	public String getTitle() {
		return CONSOLE_TITLE;
	}

	@Override
	public String getLabel() {
		return CONSOLE_LABEL;
	}

	@Override
	protected void renderContent(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
			throws IOException {
		PrintWriter pw = httpServletResponse.getWriter();
		pw.println("<div id='content' class='ui-widget'><br>");
		pw.println("<pre>");
		pw.println("Available Optimizers");
		pw.println("========================");

		Map<String, List<ServiceReference<FileOptimizer>>> optimizerCache = ((FileOptimizerServiceImpl) fileOptimizer)
				.getFileOptimizers();

		for (Entry<String, List<ServiceReference<FileOptimizer>>> to : optimizerCache.entrySet()) {

			pw.println();
			pw.println(to.getKey());
			pw.println("-------------------------------------");
			for (ServiceReference<FileOptimizer> fo : to.getValue()) {

				FileOptimizer o = this.getBundleContext().getService(fo);

				pw.println("- " + o.getName() + " (" + o.getClass().getName() + ")");
			}
		}
		pw.println("</pre>");
		pw.println("</div>");
	}

}
