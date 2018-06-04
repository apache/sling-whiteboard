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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.sling.api.resource.Resource;

/**
 * Result of file optimization
 */
public class OptimizationResult {

	private String algorithm;
	private boolean optimized = false;
	private byte[] optimizedContents;
	private long optimizedSize;
	private long originalSize;
	private final Resource resource;
	private double savings = 0;

	public OptimizationResult(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Returns the algorithm by which the file was optimized
	 * 
	 * @return the algorithm
	 */
	public String getAlgorithm() {
		return algorithm;
	}

	/**
	 * Returns the raw optimized contents as a byte array
	 * 
	 * @return the optimized contents
	 */
	public byte[] getOptimizedContents() {
		return optimizedContents;
	}

	/**
	 * Returns the optimized contents as an InputStream
	 * 
	 * @return the optimized content stream
	 */
	public InputStream getOptimizedContentStream() {
		return new ByteArrayInputStream(optimizedContents);
	}

	/**
	 * Returns the optimized size in bytes
	 * 
	 * @return the optimizedSize
	 */
	public long getOptimizedSize() {
		return optimizedSize;
	}

	/**
	 * Return the original size in bytes
	 * 
	 * @return the originalSize
	 */
	public long getOriginalSize() {
		return originalSize;
	}

	/**
	 * Returns the resource that was optimized
	 * 
	 * @return the resource
	 */
	public Resource getResource() {
		return resource;
	}

	/**
	 * Return the percent savings as a 1-based double value
	 * 
	 * @return the savings
	 */
	public double getSavings() {
		return savings;
	}

	/**
	 * Returns true if the result is actually optimized, if the optimization did not
	 * provide a smaller result or the file was not optimized for any other reason,
	 * this will be false.
	 * 
	 * @return the optimized flag
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

	public void setOptimizedContents(byte[] optimizedContents) {
		this.optimizedContents = optimizedContents;
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
