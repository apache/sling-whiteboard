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
package org.apache.sling.fileoptim;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.fileoptim.impl.FileOptimizerServiceImpl;
import org.apache.sling.fileoptim.models.OptimizedFile;
import org.apache.sling.fileoptim.optimizers.JpegFileOptimizer;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseFileOptimizerTest {

	private static final Logger log = LoggerFactory.getLogger(BaseFileOptimizerTest.class);
	public FileOptimizerServiceImpl fileOptimizerService;

	private final Map<String, InputStream> FILES = new HashMap<String, InputStream>() {
		private static final long serialVersionUID = 1L;
		{
			put("jpeg", BaseFileOptimizerTest.class.getClassLoader()
					.getResourceAsStream("valentino-funghi-41239-unsplash.jpg"));
			put("png", BaseFileOptimizerTest.class.getClassLoader()
					.getResourceAsStream("Screen Shot 2018-05-29 at 4.17.20 PM.png"));
		}
	};

	public BaseFileOptimizerTest() throws InvalidSyntaxException {
		log.trace("BaseFileOptimizerTest()");
		fileOptimizerService = new FileOptimizerServiceImpl();

		// mock up the basic OSGi stuff
		ComponentContext context = Mockito.mock(ComponentContext.class);
		BundleContext bundleContext = Mockito.mock(BundleContext.class);
		Mockito.when(context.getBundleContext()).thenReturn(bundleContext);

		// allow for populating the service cache
		@SuppressWarnings("unchecked")
		ServiceReference<FileOptimizer> ref = Mockito.mock(ServiceReference.class);
		Mockito.when(ref.getProperty(FileOptimizer.MIME_TYPE)).thenReturn("image/jpeg");
		Collection<ServiceReference<FileOptimizer>> references = new ArrayList<ServiceReference<FileOptimizer>>();
		references.add(ref);
		Mockito.when(bundleContext.getServiceReferences(FileOptimizer.class, null)).thenReturn(references);

		// allow for retrieving the service
		JpegFileOptimizer jpegFileOptimizer = new JpegFileOptimizer();
		jpegFileOptimizer.activate(new org.apache.sling.fileoptim.optimizers.JpegFileOptimizer.Config() {
			{
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public float compressionLevel() {
				return 0.7f;
			}
		});
		Mockito.when(bundleContext.getService(ref)).thenReturn(jpegFileOptimizer);

		fileOptimizerService.activate(context, new org.apache.sling.fileoptim.impl.FileOptimizerServiceImpl.Config() {
			public Class<? extends Annotation> annotationType() {
				return null;
			}

			public String hashAlgorithm() {
				return "MD5";
			}
		});

	}

	private Resource getValidFile(String type) {
		Resource fileResource = Mockito.mock(Resource.class);
		Mockito.when(fileResource.getName()).thenReturn("file." + type);

		Resource contentResource = Mockito.mock(Resource.class);
		Mockito.when(fileResource.getChild(JcrConstants.JCR_CONTENT)).thenReturn(contentResource);

		OptimizedFile of = Mockito.mock(OptimizedFile.class);
		Mockito.when(contentResource.adaptTo(OptimizedFile.class)).thenReturn(of);
		Mockito.when(of.getHash()).thenReturn(null);
		Mockito.when(of.getContent()).thenReturn(FILES.get(type));
		Mockito.when(of.getDisabled()).thenReturn(false);
		Mockito.when(of.getMimeType()).thenReturn("image/" + type);
		return fileResource;
	}

	public Resource getOptimizedFile() {
		Resource fileResource = Mockito.mock(Resource.class);
		Mockito.when(fileResource.getName()).thenReturn("file.png");

		Resource contentResource = Mockito.mock(Resource.class);
		Mockito.when(fileResource.getChild(JcrConstants.JCR_CONTENT)).thenReturn(contentResource);

		OptimizedFile of = Mockito.mock(OptimizedFile.class);
		Mockito.when(contentResource.adaptTo(OptimizedFile.class)).thenReturn(of);
		Mockito.when(of.getHash()).thenReturn(null);
		Mockito.when(of.getContent()).thenReturn(FILES.get("png"));
		Mockito.when(of.getDisabled()).thenReturn(false);
		Mockito.when(of.getMimeType()).thenReturn("image/png");
		Mockito.when(of.getHash()).thenReturn("M8SdtVLx2VXMDtteUPLVkQ==");
		return fileResource;
	}

	public Resource getDisabled() {
		Resource fileResource = Mockito.mock(Resource.class);
		Mockito.when(fileResource.getName()).thenReturn("file.jpg");

		Resource contentResource = Mockito.mock(Resource.class);
		Mockito.when(fileResource.getChild(JcrConstants.JCR_CONTENT)).thenReturn(contentResource);

		OptimizedFile of = Mockito.mock(OptimizedFile.class);
		Mockito.when(contentResource.adaptTo(OptimizedFile.class)).thenReturn(of);
		Mockito.when(of.getDisabled()).thenReturn(true);
		Mockito.when(of.getMimeType()).thenReturn("image/jpeg");
		return fileResource;
	}

	public Resource getValidJpegFile() {
		return getValidFile("jpeg");
	}

	public Resource getValidPngFile() {
		return getValidFile("png");
	}
}
