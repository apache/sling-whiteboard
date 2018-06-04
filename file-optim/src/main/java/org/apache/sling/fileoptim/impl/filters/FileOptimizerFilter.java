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
package org.apache.sling.fileoptim.impl.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.fileoptim.FileOptimizerService;
import org.apache.sling.fileoptim.OptimizationResult;
import org.apache.sling.fileoptim.impl.filters.FileOptimizerFilter.Config;
import org.apache.sling.fileoptim.models.OptimizedFile;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling Servlet Filter for optimizing a file response.
 */
@Component(service = { Filter.class }, property = { "sling.filter.scope="
		+ EngineConstants.FILTER_SCOPE_REQUEST }, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = Config.class)
public class FileOptimizerFilter implements Filter {

	@ObjectClassDefinition(name = "%filter.name", description = "%filter.description", localization = "OSGI-INF/l10n/bundle")
	public @interface Config {

		@AttributeDefinition(name = "%filter.pattern.name", description = "%filter.pattern.description")
		String sling_filter_pattern() default "-";

		@AttributeDefinition(name = "%filter.service.ranking.name", description = "%filter.service.ranking.name")
		int service_ranking() default 0;

	}

	private static final Logger log = LoggerFactory.getLogger(FileOptimizerFilter.class);

	@Reference
	private FileOptimizerService fileOptimizer;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (request instanceof SlingHttpServletRequest) {
			Resource resource = ((SlingHttpServletRequest) request).getResource();
			try {
				if (fileOptimizer.canOptimize(resource)) {
					log.debug("Returning optimized file");
					OptimizationResult res = fileOptimizer.getOptimizedContents(resource);
					if (res.isOptimized()) {
						OptimizedFile of = null;
						if (!resource.getName().equals(JcrConstants.JCR_CONTENT)) {
							of = resource.getChild(JcrConstants.JCR_CONTENT).adaptTo(OptimizedFile.class);
						} else {
							of = resource.adaptTo(OptimizedFile.class);
						}
						response.setContentType(of.getMimeType());
						response.setContentLengthLong(res.getOptimizedSize());
						((HttpServletResponse) response).setHeader("Optimized-With", res.getAlgorithm());
						IOUtils.copy(res.getOptimizedContentStream(), response.getOutputStream());
						return;
					}
				}
			} catch (Exception e) {
				log.warn("Unexpected exception attempting to optimize file response", e);
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}

}
