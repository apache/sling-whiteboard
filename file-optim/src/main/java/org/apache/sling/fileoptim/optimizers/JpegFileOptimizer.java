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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.sling.fileoptim.FileOptimizer;
import org.apache.sling.fileoptim.optimizers.JpegFileOptimizer.Config;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default optimizer which lossily compresses a JPEG image.
 */
@Component(service = FileOptimizer.class, property = { FileOptimizer.MIME_TYPE + "=image/jpeg" })
@Designate(ocd = Config.class)
public class JpegFileOptimizer implements FileOptimizer {

	private static final Logger log = LoggerFactory.getLogger(JpegFileOptimizer.class);

	@ObjectClassDefinition(name = "%jpeg.optimizer.name", description = "%jpeg.optimizer.description", localization = "OSGI-INF/l10n/bundle")
	public @interface Config {
		@AttributeDefinition(name = "%jpeg.optimizer.compression.level.name", description = "%jpeg.optimizer.compression.level.description")
		float compressionLevel() default 0.8f;
	}

	private Config config;

	@Activate
	@Modified
	public void activate(Config config) {
		this.config = config;
	}

	@Override
	public byte[] optimizeFile(byte[] original, String metaType) {
		ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
		ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
		jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgWriteParam.setCompressionQuality(config.compressionLevel());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageOutputStream outputStream = new MemoryCacheImageOutputStream(baos);
		jpgWriter.setOutput(outputStream);
		try {
			IIOImage outputImage = new IIOImage(ImageIO.read(new ByteArrayInputStream(original)), null, null);
			jpgWriter.write(null, outputImage, jpgWriteParam);
			jpgWriter.dispose();
			return baos.toByteArray();
		} catch (IOException e) {
			log.warn("Exception optimizing image", e);
		}
		return null;
	}

	@Override
	public String getName() {
		return "Apache Sling JPEG File Optimizer";
	}

}
