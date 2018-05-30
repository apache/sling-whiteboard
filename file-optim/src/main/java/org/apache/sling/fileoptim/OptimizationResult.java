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

import org.apache.sling.api.resource.Resource;

/**
 * Result of file optimization
 */
public class OptimizationResult {

	private String algorithm;
	private boolean optimized = false;
	private long optimizedSize;
	private long originalSize;
	private final Resource resource;
	private double savings = 0;

	public OptimizationResult(Resource resource) {
		this.resource = resource;
	}

	/**
	 * @return the algorithm
	 */
	public String getAlgorithm() {
		return algorithm;
	}

	/**
	 * @return the optimizedSize
	 */
	public long getOptimizedSize() {
		return optimizedSize;
	}

	/**
	 * @return the originalSize
	 */
	public long getOriginalSize() {
		return originalSize;
	}

	/**
	 * @return the resource
	 */
	public Resource getResource() {
		return resource;
	}

	/**
	 * @return the savings
	 */
	public double getSavings() {
		return savings;
	}

	/**
	 * @return the optimized
	 */
	public boolean isOptimized() {
		return optimized;
	}

	/**
	 * @param algorithm
	 *            the algorithm to set
	 */
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * @param optimized
	 *            the optimized to set
	 */
	public void setOptimized(boolean optimized) {
		this.optimized = optimized;
	}

	/**
	 * @param optimizedSize
	 *            the optimizedSize to set
	 */
	public void setOptimizedSize(long optimizedSize) {
		this.optimizedSize = optimizedSize;
	}

	/**
	 * @param originalSize
	 *            the originalSize to set
	 */
	public void setOriginalSize(long originalSize) {
		this.originalSize = originalSize;
	}

	/**
	 * @param savings
	 *            the savings to set
	 */
	public void setSavings(double savings) {
		this.savings = savings;
	}
}
