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
package org.apache.sling.fileoptim.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.fileoptim.FileOptimizerService;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.PostResponse;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The File Optimization operation will optimize a file
 */
@Component(immediate = true, service = { PostOperation.class }, property = PostOperation.PROP_OPERATION_NAME
		+ "=fileoptim:optimize")
public class OptimizeFileOperation implements PostOperation {

	private static final Logger log = LoggerFactory.getLogger(OptimizeFileOperation.class);

	@Reference
	private FileOptimizerService fileOptimizer;

	protected void doRun(SlingHttpServletRequest request, PostResponse response, List<Modification> changes)
			throws IOException {
		Resource resource = request.getResource();
		if (fileOptimizer.canOptimize(resource)) {
			fileOptimizer.optimizeFile(resource, true);
			changes.add(Modification.onModified(resource.getPath()));
		}
	}

	public void run(final SlingHttpServletRequest request, final PostResponse response,
			final SlingPostProcessor[] processors) {

		try {
			// calculate the paths
			String path = request.getResource().getPath();
			response.setPath(path);

			final List<Modification> changes = new ArrayList<>();

			doRun(request, response, changes);

			// invoke processors
			if (processors != null) {
				for (SlingPostProcessor processor : processors) {
					processor.process(request, changes);
				}
			}

			// check modifications for remaining postfix and store the base path
			final Map<String, String> modificationSourcesContainingPostfix = new HashMap<>();
			final Set<String> allModificationSources = new HashSet<>(changes.size());
			for (final Modification modification : changes) {
				final String source = modification.getSource();
				if (source != null) {
					allModificationSources.add(source);
					final int atIndex = source.indexOf('@');
					if (atIndex > 0) {
						modificationSourcesContainingPostfix.put(source.substring(0, atIndex), source);
					}
				}
			}
		} catch (Exception e) {
			log.error("Exception during response processing.", e);
			response.setError(e);
		}
	}

}
