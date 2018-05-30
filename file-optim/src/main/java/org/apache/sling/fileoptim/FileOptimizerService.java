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
package org.apache.sling.fileoptim;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;

/**
 * A service for optimizing files stored in Sling Resources.
 */
public interface FileOptimizerService {

	/**
	 * Returns true if the resource is a registered type, the CA Config for the
	 * resource is enabled and if an optimizer is registered for the file's meta
	 * type
	 * 
	 * @param fileResource
	 *            the resource to check if the optimizer is available
	 * @return true if the file optimizer can optimize the file, false otherwise
	 */
	boolean canOptimize(Resource fileResource);

	/**
	 * Optimizes a file resource.
	 * 
	 * @param fileResource
	 *            the resource to optimize
	 * @param autoCommit
	 *            if true, the results will automatically be committed to the Sling
	 *            Repo
	 * @return the results of the optimization
	 * @throws PersistenceException
	 *             an exception occurs saving the optimized resource
	 * @throws IOException 
	 *             an exception occurs reading the original resource
	 */
	OptimizationResult optimizeFile(Resource fileResource, boolean autoCommit) throws PersistenceException, IOException;

	/**
	 * Optimizes a collection of file resources.
	 * 
	 * @param fileResources
	 *            the resources to optimize
	 * @param autoCommit
	 *            if true, the results will automatically be committed to the Sling
	 *            Repo
	 * @return the results of the optimization
	 * @throws PersistenceException
	 *             an exception occurs saving the optimized resources
	 * @throws IOException 
	 *             an exception occurs reading the original resources
	 */
	Map<String, OptimizationResult> optimizeFiles(Collection<Resource> fileResources, boolean autoCommit)
			throws PersistenceException, IOException;

}
