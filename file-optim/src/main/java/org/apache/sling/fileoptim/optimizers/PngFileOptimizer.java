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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.sling.fileoptim.FileOptimizer;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.pngtastic.core.PngImage;
import com.googlecode.pngtastic.core.PngOptimizer;

/**
 * Default optimizer which lossly compresses a PNG image.
 */
@Component(service = FileOptimizer.class, property = { FileOptimizer.MIME_TYPE + "=image/png" })
public class PngFileOptimizer implements FileOptimizer {

	private static final Logger log = LoggerFactory.getLogger(PngFileOptimizer.class);

	@Override
	public byte[] optimizeFile(byte[] original, String metaType) {

		PngOptimizer optimizer = new PngOptimizer();

		PngImage image = new PngImage(new ByteArrayInputStream(original));
		try {
			PngImage optimized = optimizer.optimize(image);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			optimized.writeDataOutputStream(baos);

			return baos.toByteArray();
		} catch (IOException e) {
			log.warn("Exception optimizing PNG image", e);
		}
		return null;
	}

	@Override
	public String getName() {
		return "PNGTastic PNG Optimizer";
	}

}
