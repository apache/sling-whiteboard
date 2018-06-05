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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.sling.fileoptim.BaseFileOptimizerTest;
import org.apache.sling.fileoptim.OptimizationResult;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestFileOptimizer extends BaseFileOptimizerTest {

	public TestFileOptimizer() throws InvalidSyntaxException {
		super();
	}

	private static Logger log = LoggerFactory.getLogger(TestFileOptimizer.class);

	@Test
	public void testCanOptimize() {
		log.info("testCanOptimize");
		assertTrue(fileOptimizerService.canOptimize(getValidJpegFile()));
	}

	@Test
	public void testCantOptimize() {
		log.info("testCantOptimize");
		assertFalse(fileOptimizerService.canOptimize(getValidPngFile()));
	}

	@Test
	public void testDisabled() {
		log.info("testDisabled");
		assertFalse(fileOptimizerService.canOptimize(getDisabled()));
	}
	

	@Test
	public void isOptimized() {
		log.info("isOptimized");
		assertTrue(fileOptimizerService.isOptimized(super.getOptimizedFile()));
	}

	@Test
	public void testGetOptimizedContents() throws IOException {
		log.info("testGetOptimizedContents");
		OptimizationResult res = fileOptimizerService.getOptimizedContents(getValidJpegFile());
		assertNotNull(res);
		assertTrue(res.isOptimized());
		assertNotNull(res.getOptimizedContents());
		assertEquals(res.getOptimizedContents().length, res.getOptimizedSize());
		assertTrue(res.getSavings() > 0.0 && res.getSavings() < 1.0);
	}

}
