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
package org.apache.sling.fileoptim.optimizers;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.fileoptim.optimizers.PngFileOptimizer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPngFileOptimizer {

	private PngFileOptimizer optimizer = new PngFileOptimizer();

	private static final Logger log = LoggerFactory.getLogger(TestPngFileOptimizer.class);

	@Test
	public void testOptimizer() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copy(getClass().getClassLoader().getResourceAsStream("Screen Shot 2018-05-29 at 4.17.20 PM.png"), baos);
		byte[] optimized = optimizer.optimizeFile(baos.toByteArray(), "image/png");

		assertTrue(baos.toByteArray().length > optimized.length);

		log.info("Original size: {}", baos.toByteArray().length);
		log.info("Optimized size: {}", optimized.length);

		double savings = 1.0 - ((double) optimized.length / (double) baos.toByteArray().length);
		log.info("Compressed by {}%", Math.round(savings * 100.0));

		IOUtils.write(optimized, new FileOutputStream("target/optimized.png"));
	}
}
