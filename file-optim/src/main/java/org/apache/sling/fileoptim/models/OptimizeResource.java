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
package org.apache.sling.fileoptim.models;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.fileoptim.FileOptimizerService;
import org.apache.sling.fileoptim.OptimizationResult;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sling model for executing the optimizer on a resource, will not commit the
 * result
 */
@Model(adaptables = Resource.class)
public class OptimizeResource {

	private static final Logger log = LoggerFactory.getLogger(OptimizeResource.class);

	private boolean canOptimize;

	@OSGiService
	private FileOptimizerService fileOptimizer;

	private OptimizationResult result;

	private Resource resource;

	public OptimizeResource(Resource resource) {
		this.resource = resource;
	}

	@PostConstruct
	public void init() throws PersistenceException, IOException {
		log.debug("initializing with resource {}", resource);
		if (fileOptimizer.canOptimize(resource)) {
			this.canOptimize = true;
			this.result = fileOptimizer.getOptimizedContents(resource);
		} else {
			this.canOptimize = false;
			this.result = null;
		}
	}

	/**
	 * Returns true if the file is optimized, false otherwise
	 * 
	 * @return
	 */
	public boolean isOptimized() {
		return fileOptimizer.isOptimized(resource);
	}

	/**
	 * Gets the optimization result.
	 * 
	 * @return
	 */
	public OptimizationResult getResult() {
		return result;
	}

	/**
	 * Return true if the file can be optimized
	 * 
	 * @return
	 */
	public boolean isCanOptimize() {
		return canOptimize;
	}
}
