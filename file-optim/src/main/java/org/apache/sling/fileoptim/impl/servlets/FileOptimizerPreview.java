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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.fileoptim.FileOptimizerService;
import org.apache.sling.fileoptim.OptimizationResult;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Servlet for displaying a preview of an optimized image.
 */
@Component(service = Servlet.class, property = { "sling.servlet.paths=/system/fileoptim/preview",
		"sling.servlet.methods=GET" }, immediate = true)
public class FileOptimizerPreview extends SlingSafeMethodsServlet {

	@Reference
	private FileOptimizerService fileOptimizer;

	private static final long serialVersionUID = 8635343288414416865L;

	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		String path = request.getParameter("path");

		Resource resource = request.getResourceResolver().getResource(path);

		if (resource == null) {
			response.sendError(404, "No Resource found at path " + path);
		} else if (fileOptimizer.canOptimize(resource)) {
			OptimizationResult res = fileOptimizer.getOptimizedContents(resource);
			ValueMap vm = res.getResource().getValueMap();
			response.setContentType(vm.get(JcrConstants.JCR_MIMETYPE, String.class));
			response.setHeader("Content-disposition", "inline; filename=" + resource.getName());
			IOUtils.copy(vm.get(JcrConstants.JCR_DATA, InputStream.class), response.getOutputStream());
		} else {
			response.sendError(400, "Resource at path " + path + " is not a file or cannot be optimized");
		}
	}
}
