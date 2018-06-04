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
	 * Gets the optimized contents of a file resource. This will not update the
	 * underlying resource, but instead just returns the results of optimizing the
	 * resource.
	 * 
	 * @param fileResource
	 *            the resource to optimize
	 * @return the results of the optimization
	 * @throws IOException
	 *             an exception occurs reading the original resource
	 */
	OptimizationResult getOptimizedContents(Resource fileResource) throws IOException;

	/**
	 * Returns true if the specified resource has already been optimized by the
	 * FileOptimizer.
	 * 
	 * @param fileResource
	 *            the resource to check
	 * @return true if optimized by the file optimizer, false otherwise
	 */
	boolean isOptimized(Resource fileResource);

	/**
	 * Optimizes a file resource. This method will modify the underlying resource.
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

}
